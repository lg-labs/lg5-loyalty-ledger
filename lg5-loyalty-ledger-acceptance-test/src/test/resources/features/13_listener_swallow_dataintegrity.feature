# REQ-015: Kafka listener swallows DataIntegrityViolationException raised
# by the dedup unique index on a duplicate inbound event. The swallow
# is observable as: the duplicate produces no SECOND movement AND the
# broker does NOT redeliver the batch (which would surface as the
# movement count climbing over time as the listener consumes the same
# offset repeatedly).
#
# This is the ONE scenario that cannot be driven through the input port
# — the swallow happens in the listener, not in the handler. We use the
# real Avro producer (KafkaTestSupport) to enter through the listener.
Feature: Kafka listener swallows duplicate-event exceptions

  Scenario: Republishing the same OrderPaid event id appends no second movement
    Given a fresh customer
    When OrderPaid "12.00" is published to Kafka for order "order-A" with event "evt-paid-1"
    And the same OrderPaid "12.00" is republished to Kafka for order "order-A" with event "evt-paid-1"
    Then the listener swallowed the duplicate without rethrowing
