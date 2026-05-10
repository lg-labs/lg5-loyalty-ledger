# REQ-013: append-only HTTP surface. The read-side controller exposes ONLY
# GET; PUT / POST / DELETE / PATCH against the same URIs MUST surface
# 405 Method Not Allowed (Spring's default DispatcherServlet behaviour for
# an unmapped HTTP method on a registered URI).
#
# This is a SURFACE assertion — the deeper "no public mutating port"
# invariant lives in the architecture / RULE-013 review and is verified
# by inspection in the m5-final-review.md (TASK-020).
Feature: The read-side surface is append-only — no mutating HTTP verbs

  Scenario Outline: <verb> <endpoint> is rejected with 405
    Given a fresh customer
    When I "<verb>" the <endpoint> endpoint
    Then the response status is 405

    Examples:
      | verb   | endpoint  |
      | PUT    | balance   |
      | POST   | balance   |
      | DELETE | balance   |
      | PATCH  | balance   |
      | PUT    | movements |
      | POST   | movements |
      | DELETE | movements |
      | PATCH  | movements |
