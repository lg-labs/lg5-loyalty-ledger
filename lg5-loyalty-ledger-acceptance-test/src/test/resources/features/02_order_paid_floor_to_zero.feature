# REQ-002: floor-to-zero. An OrderPaid below 1 EUR (eg. 0.50) credits 0 — no
# delta, no movement is appended (RULE-013 append-only allows zero rows when
# the conceptual delta is 0). Balance stays at 0; we DO still record the
# event in processed_input_event for dedup but no Movement row is written.
#
# NOTE on assertion shape: we cannot assert "no outbox row" universally
# because other scenarios may have queued unrelated rows; we assert by
# event-alias instead. For the same reason we assert "0 movements TOTAL on
# this order" not on this customer.
Feature: OrderPaid below 1 EUR floors to zero credit

  Scenario: 0.50 EUR OrderPaid produces no movement and no outbound event
    Given a fresh customer
    When OrderPaid "0.50" arrives for order "order-A" with event "evt-paid-tiny"
    Then the customer balance is 0
    And the customer has 0 movements on order "order-A"
    And exactly 0 outbox rows carry event "evt-paid-tiny"
