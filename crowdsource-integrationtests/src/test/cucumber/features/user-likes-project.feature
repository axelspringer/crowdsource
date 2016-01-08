Feature: User can like and unlike projects

  Scenario: A user likes a project in the project list view.
    Given a project is available
    And another user liked the project
    And a user is logged in
    And the index page is visited
    And the user sees that the project on the list page has 1 likes
    And the user sees that he didn't liked the project on the list page.
    When the user clicks on the like button of this project on the list page,
    And the user sees that the project on the list page has 2 likes
    When the index page is visited again
    Then the user sees that the project on the list page has 2 likes
    And the user sees that he liked the project on the list page.

  Scenario: A user un-likes a project in the project list view.
    Given a project is available
    And the user liked the project
    And another user liked the project
    And a user is logged in
    And the index page is visited
    And the user sees that the project on the list page has 2 likes
    And the user sees that he liked the project on the list page.
    When the user clicks on the like button of this project on the list page,
    And the user sees that the project on the list page has 1 likes
    When the index page is visited again
    Then the user sees that the project on the list page has 1 likes
    And the user sees that he didn't liked the project on the list page.

  Scenario: A user likes a project in the project details view.
    Given a project is available
    And another user liked the project
    And a user is logged in
    And the project detail page of this project is requested
    And the user sees that the project on the detail page has 1 likes
    And the user sees that he didn't liked the project on the detail page.
    When the user clicks on the like button of this project on the detail page,
    And the user sees that the project on the detail page has 2 likes
    And the user sees that he liked the project on the detail page.

  Scenario: A user un-likes a project in the project detail view.
    Given a project is available
    And the user liked the project
    And another user liked the project
    And a user is logged in
    And the project detail page of this project is requested
    And the user sees that the project on the detail page has 2 likes
    And the user sees that he liked the project on the detail page.
    When the user clicks on the like button of this project on the detail page,
    And the user sees that the project on the detail page has 1 likes
    And the project detail page of this project is requested
    Then the user sees that the project on the detail page has 1 likes
    And the user sees that he didn't liked the project on the detail page.
