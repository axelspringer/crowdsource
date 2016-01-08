Feature: Project details

  Scenario: A user opens a project detail page
    Given a project is available
    And a user is logged in
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    And the "publish"-button is not visible
    And the "reject"-button is not visible
    And the "defer"-button is not visible
    And the "publish-defer"-button is not visible

  Scenario: An admin opens a project detail page and publishes and rejects a project
    Given a project is available
    And there is no financing round active
    And an admin is logged in
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    And the "publish"-button is visible
    And the "to-pledging-form"-button displays the text "VORGESCHLAGEN"
    And the "reject"-button is visible
    And the "defer"-button is visible
    And the "publish-defer"-button is visible
    When the "publish"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "publish"-button to disappear
    And the "to-pledging-form"-button displays the text "ZUR FINANZIERUNG"
    Then the "publish"-button is not visible
    And the "defer"-button is visible
    And the "publish-defer"-button is visible
    When the "reject"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "reject"-button to disappear
    And the "to-pledging-form"-button displays the text "ABGELEHNT"
    Then the "reject"-button is not visible
    And the "defer"-button is not visible
    And the "publish"-button is visible


  Scenario: An admin opens a project detail page and defers a project
    Given a project is available
    And there is no financing round active
    And an admin is logged in
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    And the "publish"-button is visible
    And the "reject"-button is visible
    And the "defer"-button is visible
    And the "publish-defer"-button is visible
    When the "defer"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "defer"-button to disappear
    Then the "defer"-button is not visible
    And the "publish-defer"-button is visible
    And the "reject"-button is not visible
    And the "publish"-button is visible
    And the "to-pledging-form"-button displays the text "ZURÜCKGESTELLT"
    When the "publish"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "publish"-button to disappear
    Then the "publish"-button is not visible
    And the "to-pledging-form"-button displays the text "ZUR FINANZIERUNG"
    And the "reject"-button is visible
    And the "defer"-button is visible
    And the "publish-defer"-button is visible
    When the "defer"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "defer"-button to disappear
    When the "publish"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "publish"-button to disappear
    Then the "defer"-button is visible
    And the "publish-defer"-button is visible

  Scenario: An admin defers a project, a financing round has taken place and eventually the project is published
    Given a project is available
    And an admin is logged in
    And there is no financing round active
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    When the "defer"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "defer"-button to disappear
    Then the "publish"-button is visible
    And the "defer"-button is not visible
    And the "publish-defer"-button is visible
    And the "to-pledging-form"-button displays the text "ZURÜCKGESTELLT"
    When there is a financing round active for 5 seconds
    And the user waits for the end of the financing round
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    And the "to-pledging-form"-button displays the text "ZUR FINANZIERUNG"
    And the "defer"-button is visible again
    And the "publish-defer"-button is visible again
    And the "publish"-button is not visible

  Scenario: An admin publicly defers a project, a financing round has taken place and eventually the project is published
    Given a project is available
    And an admin is logged in
    And there is no financing round active
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    When the "publish-defer"-button is clicked
    And the confirmation dialog is accepted
    And the user waits for the "publish-defer"-button to disappear
    Then the "publish"-button is visible
    And the "defer"-button is visible
    And the "publish-defer"-button is not visible
    And the "to-pledging-form"-button displays the text "ZURÜCKGESTELLT"
    When there is a financing round active for 5 seconds
    And the user waits for the end of the financing round
    And the index page is visited
    When the user clicks on the tile of this project
    Then the project detail page of this project is displayed
    And the "to-pledging-form"-button displays the text "ZUR FINANZIERUNG"
    And the "defer"-button is visible again
    And the "publish-defer"-button is visible again
    And the "publish"-button is not visible

  Scenario: A user opens a project detail page with an invalid project id
    Given the user requests the project detail page with a non existant project id
    Then the Not Found error page is displayed

  Scenario: A user clicks the funding link to got to the funding widget
    Given the user is on a project detail page
    When the user clicks the funding button in status widget
    Then the browser scrolls to the funding widget