Feature: Crowdsource platform statistics

  Scenario Outline: An admin views the statistics of new registrations and project creations
    Given an admin is logged in
    And she directly opens the statistics page.
    When she selects the statistic <statistic-type>
    Then all expected UI elements are displayed for the selected statistic.
  Examples:
    |statistic-type                                           |
    | 'Anzahl Neuregistrieung / Neu eingereichte Ideen'       |
    | 'Projekte je Projektstatus'                             |



