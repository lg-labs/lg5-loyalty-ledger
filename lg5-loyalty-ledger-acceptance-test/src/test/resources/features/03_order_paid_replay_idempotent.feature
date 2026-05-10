# REQ-003: idempotency of OrderPaid replay (same originatingEventId twice).
# The listener-side dedup is REQ-015; this scenario asserts the application-
# level invariant that a replay leaves balance + movements + outbox unchanged.
#
# Achieves the assertion at the input-port layer: the second process(...)
# call with the same eventId raises DataIntegrityViolationException from the
# uq_processed_event_type_id constraint (ADR-003). LedgerSteps swallows it
# (mirroring the listener) so the Then steps can verify the no-side-effect
# invariant.
Feature: OrderPaid replay is idempotent

  Scenario: Replaying an already-processed OrderPaid does not double-credit
    Given a fresh customer
    When OrderPaid "12.00" arrives for order "order-A" with event "evt-paid-1"
    And OrderPaid "12.00" arrives for order "order-A" with event "evt-paid-1"
    Then the customer balance is 12
    And the customer has 1 movement on order "order-A"
    And exactly 1 outbox row carry event "evt-paid-1"
