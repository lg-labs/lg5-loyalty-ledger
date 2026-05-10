# REQ-001 (credit on OrderPaid, floor to int EUR), REQ-011 (outbound published),
# REQ-012 (outbound payload fields), REQ-014 (origin trace into payload).
# Mirrors LoyaltyLedgerHandlerHappyPathIT.caseA at the ATDD layer.
Feature: OrderPaid credits the customer and queues an outbound event

  Scenario: A 12.95 EUR OrderPaid arrives for a fresh customer
    Given a fresh customer
    When OrderPaid "12.95" arrives for order "order-A" with event "evt-paid-1"
    Then the customer balance is 12
    And the customer has 1 movement on order "order-A"
    And a movement was appended with delta 12 and cause "ORDER_PAID" on order "order-A"
    And exactly 1 outbox row carry event "evt-paid-1"
    And the outbox payload for event "evt-paid-1" has newBalance=12 delta=12 cause="ORDER_PAID" originatingEventType="OrderPaid"
