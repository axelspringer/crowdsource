Feature: Pledge project

  Scenario: An anonymous user tries to pledge a project
    Given a project is available
    And an admin publishs the created project
    And there is no financing round active
    When the project detail page of this project is requested
    Then the notification message "Momentan läuft keine Finanzierungsrunde. Bitte versuche es nochmal, wenn die Finanzierungsrunde gestartet worden ist." is displayed on the project pledging form
    And the project pledging form is disabled
    And the user budget 0 is displayed
    When a financing round is being activated in the meantime
    And the current page is reloaded
    Then the notification message "Bitte logge dich ein, um Projekte finanziell zu unterstützen." is displayed on the project pledging form
    And the project pledging form is disabled
    And the user budget 0 is displayed

  Scenario: A user pledges a project
    Given a project is published
    And a user is logged in
    And there is a financing round active
    When the project detail page of this project is requested
    Then there is no notification message
    When the user raises his desired pledge amount via the slider
    Then the displayed budget and financing infos are updated
    When the user submits the pledging form
    Then the notification message "Deine Finanzierung war erfolgreich." is displayed on the project pledging form
    And the project pledging form is enabled

  Scenario: A user reduces pledges she already made for a project
    Given a project with a pledge goal of 250 is published
    And a user is logged in
    And there is a financing round active
    And the project detail page of this project is requested
    And the user enters 240 as his desired pledge amount
    And the user submits the pledging form
    And the number of backers is displayed with a value of 1
    When the user enters 200 as his desired pledge amount
    Then the displayed budget and financing infos are updated
    When the user submits the pledging form
    Then the notification message "Budget erfolgreich aus dem Projekt abgezogen." is displayed on the project pledging form
    And the project pledging form is enabled
    And the number of backers is displayed with a value of 1

  Scenario: A user reduces pledges she already made to zero for a project
    Given a project with a pledge goal of 250 is published
    And a user is logged in
    And there is a financing round active
    And the project detail page of this project is requested
    And the user enters 240 as his desired pledge amount
    And the user submits the pledging form
    And the project detail page of this project is requested again
    Then there is no notification message
    And the number of backers is displayed with a value of 1
    When the user enters 0 as his desired pledge amount
    Then the displayed budget and financing infos are updated
    And the user submits the pledging form
    Then the notification message "Budget erfolgreich aus dem Projekt abgezogen." is displayed on the project pledging form
    And the project pledging form is enabled
    And the number of backers is displayed with a value of 0

  Scenario: A user fully pledges a project
    Given a project is published
    And a user is logged in
    And there is a financing round active
    When the project detail page of this project is requested
    When the user sets his desired pledge amount as high as the remaining amount of the project goal
    Then the displayed budget and financing infos are updated
    When the user submits the pledging form
    Then the notification message "Deine Finanzierung war erfolgreich. Das Projekt ist jetzt zu 100% finanziert. Eine weitere Finanzierung ist nicht mehr möglich." is displayed on the project pledging form
    Then the displayed budget and financing infos are updated
    And the current page is reloaded
    Then the notification message "Das Projekt ist zu 100% finanziert. Eine weitere Finanzierung ist nicht mehr möglich." is displayed on the project pledging form
    When he clicks on the Logout button
    And the project detail page of this project is requested
    # an anonymous user should not get the message "Bitte logge dich ein, um Projekte finanziell zu unterstützen." in this state
    Then the notification message "Das Projekt ist zu 100% finanziert. Eine weitere Finanzierung ist nicht mehr möglich." is displayed on the project pledging form

  Scenario: The finance round is stopped while a user pledges a project
    Given a project is published
    And there is a financing round active
    And a user is logged in
    When the project detail page of this project is requested
    And there is no financing round active
    When the user raises his desired pledge amount via the slider
    Then the displayed budget and financing infos are updated
    When the user submits the pledging form
    Then the error message "Die Finanzierungsrunde ist mittlerweile leider beendet. Das Finanzieren ist erst wieder möglich, wenn die nächste Runde gestartet wurde." is displayed on the project pledging form
    And the project pledging form is disabled

  Scenario: A user over-pledges a project
    Given a project with a pledge goal of 25 is published
    And there is a financing round active
    And a user is logged in
    When the project detail page of this project is requested
    And another user pledges the same project with 20 in the meantime
    When the user enters 10 as his desired pledge amount
    And the user submits the pledging form
    Then the number of backers is displayed with a value of 1
    Then the error message "Das Projekt wurde mittlerweile von anderen Benutzern finanziert und deine Finanzierung hätte den Finanzierungsbedarf des Projekts überschritten. Die Projektdaten wurden soeben aktualisiert und wir bitten dich einen neuen Finanzierungsbetrag einzugeben." is displayed on the project pledging form
    And the project pledging form is enabled
    When the user enters 5 as his desired pledge amount
    And the user submits the pledging form
    Then the notification message "Deine Finanzierung war erfolgreich. Das Projekt ist jetzt zu 100% finanziert. Eine weitere Finanzierung ist nicht mehr möglich." is displayed on the project pledging form
    Then the number of backers is displayed with a value of 2

  Scenario: An admin pledges a project from post round budget after the last round terminated
    Given a project with a pledge goal of 200 is published
    And there is a financing round active with a budget of 10000
    And a user is logged in
    And the project detail page of this project is requested
    And the user enters 58 as his desired pledge amount
    And the user submits the pledging form
    And the project detail page of this project is requested again
    And he clicks on the Logout button
    When an admin is logged in
    And there is no financing round active
    And we wait a second for the round to be post processed
    And the project detail page of this project is requested again
    Then the project pledging form is enabled
    And the notification message "Momentan läuft keine Finanzierungsrunde. Du bist als Admin jedoch berechtigt aus dem restlichen Budget der Finanzierungsrunde weitere Investments zu tätigen." is displayed on the project pledging form
    And the displayed user budget is 0
    And the displayed post round budget is 9942
    When the user enters 42 as his desired pledge amount
    And the user submits the pledging form
    Then the displayed user budget is 0
    And the displayed post round budget is 9900

   Scenario: An admin cannot post round pledge a project that has been published after the last round terminated
     Given there is a financing round active with a budget of 10000
     And there is no financing round active
     And a project with a pledge goal of 200 is published
     When an admin is logged in
     And the project detail page of this project is requested
     And the user enters 58 as his desired pledge amount
     And the user submits the pledging form
     Then the error message "Das Projekt nahm nicht an der letzten Finanzierungsrunde teil und kann daher nicht nachfinanziert werden." is displayed on the project pledging form
     When he clicks on the Logout button
     And a user is logged in
     And the project detail page of this project is requested again
     Then the project pledging form is disabled

