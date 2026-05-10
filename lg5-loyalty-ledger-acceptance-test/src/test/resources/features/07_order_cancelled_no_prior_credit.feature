# REQ-005 / Q2: Cancel/refund without a prior credit on the same orderId is
# a NO-OP. The handler writes a processed_input_event row with outcome
# NOOP_DEBIT_WITHOUT_CREDIT and emits a WARN log line; no Movement, no
# CustomerBalance update, no outbox row.
#
# We verify the observable side-effects (no movement, no outbox); the WARN
# log line itself is verified at the unit-test layer in the application-
# service module — asserting on logger output from Cucumber would require
# wiring a test appender which is overkill for the read-side invariant.
Feature: OrderCancelled without prior credit is a no-op

  Scenario: Cancel arriving before any credit on the same order does nothing
    Given a fresh customer
    When OrderCancelled arrives for order "order-A" with event "evt-orphan-cancel"
    Then the customer balance is 0
    And the customer has 0 movements on order "order-A"
    And exactly 0 outbox rows carry event "evt-orphan-cancel"
