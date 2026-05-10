# REQ-006: idempotency of OrderCancelled replay. Same shape as REQ-003 but
# on the debit path. Replay surfaces DataIntegrityViolationException from
# the dedup constraint; LedgerSteps swallows it (mirroring the listener).
Feature: OrderCancelled replay is idempotent

  Scenario: Replaying an already-processed OrderCancelled does not double-debit
    Given a fresh customer
    And the customer has been credited "12.00" on order "order-A"
    When OrderCancelled arrives for order "order-A" with event "evt-cancelled-1"
    And OrderCancelled arrives for order "order-A" with event "evt-cancelled-1"
    Then the customer balance is 0
    And the customer has 2 movements on order "order-A"
    And exactly 1 outbox row carry event "evt-cancelled-1"
