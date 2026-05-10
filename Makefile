# Makefile for lg5-loyalty-ledger
APP = lg5-loyalty-ledger-container
INFRA = lg5-loyalty-ledger-support/docker
AVRO_MODEL = lg5-loyalty-ledger-message/lg5-loyalty-ledger-message-model
ATDD = lg5-loyalty-ledger-acceptance-test
FILE_LOG ?=false
DOCKER_COMPOSE := docker-compose

build_to_arm:
	mvn clean install -Parch-aarch64
build_to_amd:
	mvn clean install -Pamd

clean:
	mvn clean
# INSTALL ARTIFACT
install: clean
	mvn install

install-skip-test: clean
	mvn install -DskipTests

install-skip-test-jib: clean
	mvn install -DskipTests -Djib.skip=true
# TESTING
run-checkstyle:
	mvn validate
run-verify: clean
	mvn verify -Dit.test="**/*IT.java,**/*Test.java" -Dfailsafe.failIfNoSpecifiedTests=false -Djib.skip=true

run-unit-test: clean
	mvn test

run-integration-test: install-skip-test-jib
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*IT.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-acceptance-test-alone: install-skip-test-jib
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*AcceptanceT*.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-acceptance-test: install-skip-test
	mvn failsafe:integration-test failsafe:verify -Dit.test="**/*IT.java" -Dfailsafe.failIfNoSpecifiedTests=false

run-test-spec-base:
	mvn failsafe:integration-test failsafe:verify -Dit.test=${TEST_NAME} -Dfailsafe.failIfNoSpecifiedTests=false

run-at-by-tag: run-acceptance-test
	-Dcucumber.filter.tags=${TAG_NAME}

run-test-spec: install-skip-test run-test-spec-base
run-ut-spec: install-skip-test-jib run-test-spec-base
run-it-spec: install-skip-test-jib run-test-spec-base
run-at-spec: run-test-spec

# SETUP INFRASTRUCTURE
docker-kill:
	@echo "Killing all Docker containers..."
	@docker ps -aq | xargs -r docker rm -f

docker-prune:
	@echo "Cleaning Docker..."
	@docker system prune --volumes --force

kafka-down: docker-kill
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml down --volumes --remove-orphans
ddbb-down: docker-kill
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml down --volumes --remove-orphans

kafka-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/kafka_cluster.yml up -d
ddbb-up:
	docker-compose -f ${INFRA}/common.yml -f ${INFRA}/postgres-ddbb.yml up -d

# DOWN ALL
docker-down: kafka-down ddbb-down docker-prune
d-down: docker-down

# UP ALL
docker-up: d-down kafka-up ddbb-up
d-up: d-down docker-up

## APPs
run-app:
	mvn -f lg5-loyalty-ledger-container/pom.xml spring-boot:run

run-apps: run-app

run-happy-path: docker-down docker-up run-app

# KAFKA MODELS from Avro Model definition
run-avro-model:
	mvn -pl ${AVRO_MODEL} clean install

# SCHEMA REGISTRY (TASK-014)
# Targets a local Confluent Schema Registry (default
# http://localhost:8081, override via SCHEMA_REGISTRY_URL=...).
# Both targets read the .avsc files directly from
# lg5-loyalty-ledger-message-model/src/main/resources/avro/.
#
#   publish-schemas      register both subjects + set BACKWARD compat
#                        (idempotent: safe to re-run)
#   check-schema-compat  read-only CI gate; non-zero exit if a local
#                        change is incompatible with the registered
#                        latest version (no registration performed)
publish-schemas:
	@./lg5-loyalty-ledger-support/scripts/publish-schemas.sh

check-schema-compat:
	@./lg5-loyalty-ledger-support/scripts/check-schema-compat.sh

run-atdd-module:
	mvn -pl ${ATDD} clean install -Dapplication.traces.file.enabled=${FILE_LOG}
