Feature: Add project

  Scenario: A user adds a new project
    Given a user is logged in
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the project creation page
    When he submits the project form with valid project data
    Then the project creation success page is shown
    When he clicks the project details link
    Then the project details page shows the new project
    When the CROWD link is clicked
    Then the project overview page shows the new project
    And the project is marked "proposed"

  Scenario: A user adds a new project and it is published by an admin
    Given a user is logged in
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the project creation page
    When he submits the project form with valid project data
    Then the project creation success page is shown
    When an admin publishs the project
    When he clicks the project details link
    Then the project details page shows the new project
    When the CROWD link is clicked
    Then the project overview page shows the new project
    And the project is marked "published"

  Scenario: A user adds a new project and it is deferred by an admin
    Given a user is logged in
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the project creation page
    When he submits the project form with valid project data
    Then the project creation success page is shown
    When an admin defers the project
    When he clicks the project details link
    Then the project details page shows the new project
    When the CROWD link is clicked
    Then the project overview page shows the new project
    And the project is marked "deferred"

  Scenario: A user views the new project page and gets a tooltip for currency conversion
    Given a user is logged in
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the project creation page
    And the tooltip for currency conversion is not visible
    When he hovers the currency element
    Then the tooltip for currency conversion is visible

  Scenario: An anonymous user tries to add a new project
    Given the index page is visited
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the login page
    When he enters valid credentials
    Then he is redirected to the project creation page
