# REQ-004 + REQ-005: OrderCancelled following a credit appends a debit equal
# in magnitude to the prior credit (v1 has no partial cancel — Q8 / data-
# model.md §Movement). Outbound payload carries the new (zero) balance and
# cause=ORDER_CANCELLED.
Feature: OrderCancelled debits the customer after a credit

  Scenario: Cancel after a 12 EUR credit appends a -12 debit and zeroes balance
    Given a fresh customer
    And the customer has been credited "12.00" on order "order-A"
    When OrderCancelled arrives for order "order-A" with event "evt-cancelled-1"
    Then the customer balance is 0
    And the customer has 2 movements on order "order-A"
    And a movement was appended with delta -12 and cause "ORDER_CANCELLED" on order "order-A"
    And the outbox payload for event "evt-cancelled-1" has newBalance=0 delta=-12 cause="ORDER_CANCELLED" originatingEventType="OrderCancelled"
