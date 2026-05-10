# REQ-003 + Q9/R5: legitimate paid → cancelled → paid-again sequence on the
# SAME orderId with DISTINCT event ids must NOT be deduplicated. Each step
# carries its own originatingEventId (different alias = different UUID per
# World contract) so the dedup constraint is not triggered.
#
# Mirrors LoyaltyLedgerHandlerHappyPathIT.caseG at the ATDD layer.
Feature: Re-paying after a cancel produces an additional credit

  Scenario: Paid → cancelled → paid again with distinct event ids finals at +12
    Given a fresh customer
    When OrderPaid "12.00" arrives for order "order-A" with event "evt-paid-1"
    And OrderCancelled arrives for order "order-A" with event "evt-cancelled-1"
    And OrderPaid "12.00" arrives for order "order-A" with event "evt-paid-2"
    Then the customer balance is 12
    And the customer has 3 movements on order "order-A"
