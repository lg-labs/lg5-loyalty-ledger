# Arquitectura de lg5-loyalty-ledger

Este documento describe la arquitectura del servicio `lg5-loyalty-ledger`, incluyendo:

1. **Arquitectura de eventos** - Diagrama de flujo de eventos con Kafka
2. **Modelo C4+1** - Arquitectura desde contexto hasta detalles técnicos

---

## 1. Arquitectura de Eventos

```mermaid
flowchart TB
    subgraph OrderService ["Microservicio de Órdenes"]
        direction TB
        OrderController[Órdenes API]
        OrderPaidEvent[OrderPaid Event]
        OrderCancelledEvent[OrderCancelled Event]
        OrderRefundedEvent[OrderRefunded Event]
    end

    subgraph Kafka ["Kafka Cluster"]
        TopicPaid[loyalty-ledger-order-paid]
        TopicCancelled[loyalty-ledger-order-cancelled]
        TopicRefunded[loyalty-ledger-order-refunded]
        TopicBalanceUpdated[loyalty-ledger-customer-balance-updated]
    end

    subgraph LoyaltyLedger ["lg5-loyalty-ledger"]
        direction TB
        ListenerPaid[Kafka Consumer - OrderPaid]
        ListenerCancelled[Kafka Consumer - OrderCancelled]
        ListenerRefunded[Kafka Consumer - OrderRefunded]
        DedupGate[Gate de Deduplicación]
        CommandPort{Input Port - Command}
        LedgerService[Domain Service]
        OutboxScheduler[Outbox Scheduler]
        QueryService[Query Service]
    end

    subgraph Database ["PostgreSQL"]
        TableMovement[(Movement)]
        TableBalance[(CustomerBalance)]
        TableOutbox[(Outbox)]
        TableProcessed[(ProcessedInputEvent)]
    end

    subgraph Api ["API REST Layer"]
        BalanceController[GET /balance]
        MovementsController[GET /movements]
    end

    OrderController -->|publica| OrderPaidEvent
    OrderController -->|publica| OrderCancelledEvent
    OrderController -->|publica| OrderRefundedEvent

    ListenerPaid -.- TopicPaid
    ListenerCancelled -.- TopicCancelled
    ListenerRefunded -.- TopicRefunded

    OrderPaidEvent --> DedupGate
    OrderCancelledEvent --> DedupGate
    OrderRefundedEvent --> DedupGate

    DedupGate --nuevo evento--> CommandPort
    DedupGate --duplicado/rechazado-.--> Ignorar

    CommandPort -->|dispatch| LedgerService

    LedgerService -->|append| TableMovement
    LedgerService -->|update| TableBalance
    LedgerService -.->|raise| CustomerBalanceUpdatedEvent

    CustomerBalanceUpdatedEvent --> OutboxScheduler

    OutboxScheduler -.reads.-> TableOutbox
    OutboxScheduler -->|publish| TopicBalanceUpdated

    TopicBalanceUpdated -->|key=customerId| QueryService

    BalanceController --> QueryService
    MovementsController --> QueryService

    style OrderService fill:#e1f5fe
    style Kafka fill:#fff3e0
    style LoyaltyLedger fill:#e8f5e9
    style Database fill:#f3e5f5
    style Api fill:#fce4ec
```

---

## 2. Modelo C4+1

### 2.1 C1 - Context Diagram (Vista de Contorno)

```mermaid
graph TB
    subgraph External["Sistemas Externos"]
        OrderSvc[Microservicio de Ordenes]
    end

    subgraph LG5Loyalty["lg5-loyalty-ledger"]
        API[API REST<br/>Port 8080]
        KafkaIn[Kafka Inbound<br/>Consumer Group]
        KafkaOut[Kafka Outbound<br/>Outbox]
    end

    subgraph Storage["Almacenamiento"]
        DB[(PostgreSQL<br/>H2 dev)]
        Kafka[(Kafka Cluster<br/>3 brokers)]
    end
    
    OrderSvc -.- KafkaIn
    API <-.-<- DB
    KafkaOut -.publishes-> Kafka
    KafkaOut -.- DB
```

### 2.2 C2 - Container Diagram (Vista de Contenedor)

```mermaid
graph TB
    subgraph Containments["Contenedores Spring Boot"]
        API[lg5-loyalty-ledger-api<br/>Spring WebFlux<br/>Port: 8080]
        APP[lg5-loyalty-ledger-application<br/>Application Services<br/>Inbound Consumers]
        OUTBOX[Outbox Scheduler<br/>Spring @Scheduled<br/>Thread-safe]
    end

    subgraph Infrastructure["Infraestructura"]
        DIR[Directory Services<br/>Client Discovery]
        DB[(PostgreSQL<br/>JPA Hibernate<br/>@Version)]
        KAFKA_IN[Kafka Consumer<br/>order-paid/cancelled/refunded]
        KAFKA_OUT[Kafka Producer<br/>Topic: customer-balance-updated]
    end

    API --> APP
    API -.read-> DB
    
    APP -->|inbound| KAFKA_IN
    APP -->|domain logic| APP
    APP -->|append| DB
    APP -->|outbox queue| DB
    
    OUTBOX -.reads-> DB
    OUTBOX -.reads-> APP
    OUTBOX -.- KAFKA_OUT
    OUTBOX -.read-> DB
    
    style API fill:#e3f2fd,stroke:#1565c0
    style APP fill:#e8f5e9,stroke:#2e7d32
    style OUTBOX fill:#fff3e0,stroke:#ef6c00
    style DB fill:#f3e5f5,stroke:#7b1fa2
    style KAFKA_IN fill:#fff9c4,stroke:#f9a825
    style KAFKA_OUT fill:#e3f2fd,stroke:#1565c0
```

### 2.3 C3 - Component Diagram (Vista de Componentes - Core)

```mermaid
classDiagram
    class Movement {
        -CustomerId customerId
        -int delta
        -BalanceUpdateCause cause
        -OrderId originatingOrderId
        -UUID originatingEventId
        -ZonedDateTime originatingEventReceivedAt
        -ZonedDateTime appendedAt
        -int version
        +CustomerId getCustomerId()
        +int getDelta()
        +BalanceUpdateCause getCause()
        +OrderId getOriginatingOrderId()
        +appendOnly()
    }

    class CustomerBalance {
        -long balance
        -ZonedDateTime lastUpdatedAt
        -int version
        +CustomerBalance(CustomerId, long, ZonedDateTime, int)
        +applyDelta(int)
        +getBalance()
        +getLastUpdatedAt()
    }

    class ProcessedInputEvent {
        -ProcessedInputEventId processedInputEventId
        -ProcessedInputEventOutcome processedInputEventOutcome
        -int delta
        -boolean deduplicated
        -String deduplicatedReason
        +ProcessedInputEvent(ProcessedInputEventId, ProcessedInputEventOutcome, int, boolean, String)
        +getProcessedInputEventId()
        +getProcessedInputEventOutcome()
        +getDelta()
        +isDeduplicated()
        +getDeduplicatedReason()
    }

    class LoyaltyLedgerInputPort {
        +applyCredit(CustomerId eventId, long delta)
        +applyDebit(CustomerId eventId, long delta, String reason)
    }

    class LoyaltyLedgerQueryService {
        +getBalance(CustomerId)
        +getMovements(CustomerId, Pagination)
    }

    class CommandBus {
        +handle(Any)
    }

    class QueryBus {
        +handle(Any)
    }

    Movement --> CustomerBalance
    Movement -.- ProcessedInputEvent
    LoyaltyLedgerInputPort -|-- CommandBus
    CommandBus -.- Movement
    CommandBus -.- CustomerBalance
    LoyaltyLedgerQueryService -|-- QueryBus
    QueryBus -.- CustomerBalance
```

### 2.4 C4 - System Architecture (Vista de Arquitectura)

```mermaid
graph TB
    subgraph External["Entidades Externas"]
        CustomerSvc[Servicio de Gestión de Clientes]
        OrderSvc[Servicio de Gestión de Órdenes]
        AuditSvc[Servicio de Auditoría/Compliance]
    end

    subgraph LG5Loyalty["lg5-loyalty-ledger"]
        API[API REST v1<br/>8080<br/>Swagger/AsyncAPI]
        
        subgraph Processing["Procesamiento de Eventos"]
            Inbound[Consumidores Inbound<br/>order-paid<br/>order-cancelled<br/>order-refunded]
            Ledger[Álgebra de Movimientos<br/>Balance Projection<br/>Aggregate: CustomerBalance]
            Outbox[Transactional Outbox<br/>@Scheduled Scheduler]
        end
        
        subgraph Querying["Consulta (CQRS Read)"]
            Projection[View Layer<br/>CustomerBalance View]
            History[Movements View<br/>Ledger History]
        end
    end

    subgraph Storage["Almacenamiento"]
        DB[PostgreSQL<br/>Tables:<br/>- movement<br/>- customer_balance<br/>- outbox<br/>- processed_input_event]
    end

    subgraph KafkaTopics["Kafka Topics"]
        InboundTopics[Topics Inbound<br/>loyalty-ledger-order-paid<br/>loyalty-ledger-order-cancelled<br/>loyalty-ledger-order-refunded]
        OutboundTopic[Topic Outbound<br/>loyalty-ledger-customer-balance-updated]
    end

    OrderSvc -->|inbound events| InboundTopics
    CustomerSvc -.->|queries| API
    API -.- Projection
    API -.- History
    AuditSvc -.->|compliance queries| History
    
    InboundTopics -->|consume| Inbound
    Inbound -->|process| Ledger
    Ledger -.- DB
    Ledger -.- Outbox
    Outbox -.- OutboundTopic
    OutboundTopic -.->|publish| InboundTopics
    
    style LG5Loyalty fill:#e8f5e9,stroke:#2e7d32,stroke-width:3px
    style API fill:#e3f2fd,stroke:#1565c0
    style Processing fill:#fff3e0,stroke:#f57f17
    style Querying fill:#e8f5e9,stroke:#1b5e20
```

---

## Resumen de Patrones

| Patrón | Descripción | Ubicación |
|--------|-------------|-----------|
| **CQRS** | Separación escritura/lectura | Application Layer |
| **Outbox** | Publicación transaccional de eventos | Outbox Scheduler + DB |
| **Event Sourcing** | Ledger inmutable como history | Movement aggregate |
| **Aggregate Root** | CustomerBalance, Movement | Domain Core |
| **Deduplication** | Gate `(messageId, eventType)` | Consumer side |
| **Transactional Outbox** | Publicación garantizada | Outbox table + scheduler |
| **AsyncAPI** | Contrato de eventos | docs/api/asyncapi.yaml |

---

## Referencias

- **Framework:** [`lg5-spring`](https://github.com/lg-labs-pentagon/lg5-spring)
- **Ejemplo real:** [`food-ordering-system`](https://github.com/lg-labs/food-ordering-system)
- **Avro Schema Registry:** [`lg5-loyalty-ledger-message`](./lg5-loyalty-ledger-message/)
- **AsyncAPI Spec:** [`docs/api/asyncapi.yaml`](./docs/api/asyncapi.yaml)
- **OpenAPI Spec:** [`docs/api/openapi.yaml`](./docs/api/openapi.yaml)

---

*Generado para `lg5-loyalty-ledger` siguiendo el workflow Spec-Driven Development.*