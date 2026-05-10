# REQ-009: GET /loyalty/customers/{id}/balance — happy path returns the
# current balance and metadata; unknown customer returns 404 with
# CUSTOMER_NOT_FOUND. REQ-016 / RULE-006: response Content-Type is the
# v1 vendor JSON for both branches (the error path is mediated by the
# @RestControllerAdvice from TASK-017 which inherits the controller-class
# produces= setting).
Feature: GET customer balance

  Scenario: Returns the current balance for a known customer
    Given a fresh customer
    And the customer has been credited "12.00" on order "order-A"
    When I GET the customer balance
    Then the response status is 200
    And the response Content-Type starts with the v1 vendor JSON
    And the response JSON balance is 12

  Scenario: Returns 404 CUSTOMER_NOT_FOUND for an unknown customer
    When I GET the balance for an unknown customer
    Then the response status is 404
    And the response Content-Type starts with the v1 vendor JSON
    And the response JSON error code is "CUSTOMER_NOT_FOUND"
