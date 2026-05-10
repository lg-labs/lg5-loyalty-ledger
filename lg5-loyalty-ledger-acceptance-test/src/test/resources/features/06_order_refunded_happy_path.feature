# REQ-004 + Q8: OrderRefunded behaves identically to OrderCancelled (full
# reversal of the prior credit, no partial refund in v1) but the cause on
# both Movement and outbound payload is ORDER_REFUNDED so downstream
# consumers can distinguish the lifecycle event.
Feature: OrderRefunded debits the customer with REFUND cause

  Scenario: Refund after a 12 EUR credit appends a -12 debit with REFUND cause
    Given a fresh customer
    And the customer has been credited "12.00" on order "order-A"
    When OrderRefunded arrives for order "order-A" with event "evt-refunded-1"
    Then the customer balance is 0
    And the customer has 2 movements on order "order-A"
    And a movement was appended with delta -12 and cause "ORDER_REFUNDED" on order "order-A"
    And the outbox payload for event "evt-refunded-1" has newBalance=0 delta=-12 cause="ORDER_REFUNDED" originatingEventType="OrderRefunded"
