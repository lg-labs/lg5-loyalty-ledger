# REQ-010: GET /loyalty/customers/{id}/movements is paged in reverse-
# chronological order. We seed 60 movements (each a 1 EUR OrderPaid → +1
# credit) so a 20-per-page request yields 3 full pages + an empty page 3,
# with totalElements=60 stable across pages.
#
# Page 0 / size 20 → 20 rows, totalElements 60
# Page 2 / size 20 → 20 rows (oldest 20), totalElements 60
# Page 3 / size 20 → 0 rows, totalElements 60 (out-of-range = 200 with
#                    empty array, NOT 404 — controller javadoc).
#
# 60 rows is enough to prove paging without inflating runtime; the
# IT-layer CustomerMovementsControllerIT exercises 25 rows with the same
# clamp + ordering invariants on a finer grid.
Feature: GET customer movements is paged

  Scenario: Three full pages of 20 movements each plus an empty out-of-range page
    Given a fresh customer
    When the customer receives 60 OrderPaid events of "1.00" each
    Then the customer balance is 60

    When I GET the movements page 0 size 20
    Then the response status is 200
    And the response Content-Type starts with the v1 vendor JSON
    And the response JSON has 20 movements and totalElements 60
    And the response JSON page is 0 and size is 20

    When I GET the movements page 2 size 20
    Then the response status is 200
    And the response JSON has 20 movements and totalElements 60
    And the response JSON page is 2 and size is 20

    When I GET the movements page 3 size 20
    Then the response status is 200
    And the response JSON has 0 movements and totalElements 60
