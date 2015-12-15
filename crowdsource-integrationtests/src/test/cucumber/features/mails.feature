Feature: Mail-Notifications

  @ClearMailServer
  Scenario: An activation email is sent to new users
    When a new user registers
    Then he receives an activation email

  @ClearMailServer
  Scenario: An email is received when a user claims to have forgotten his password
    When the user claims to have forgotten his password
    Then he receives a 'password forgotten' email

  @ClearMailServer
  Scenario: A notification on new projects is sent to the administrator
    When a new project is submitted via the HTTP-Endpoint
    Then an email notification is sent to the administrator

  @ClearMailServer
  Scenario: A notification on rejected projects is sent to the project creator
    When a new project is submitted via the HTTP-Endpoint
    And the sent mail is cleared
    And an administrator rejects the project
    Then an email notification about the rejected project is sent to the user

  @ClearMailServer
  Scenario: A notification on deferred projects is sent to the project creator
    When a new project is submitted via the HTTP-Endpoint
    And the sent mail is cleared
    And there is no financing round active
    And an administrator defers the project
    Then an email notification about the deferred project is sent to the user

  @ClearMailServer
  Scenario: A notification on published projects is sent to the project creator
    When a new project is submitted via the HTTP-Endpoint
    And the sent mail is cleared
    And an administrator publishes the project
    Then an email notification about the published project is sent to the user

  @ClearMailServer
  Scenario: A notification on a commented project is sent to the project creator
    When a new project is submitted via the HTTP-Endpoint
    And the sent mail is cleared
    And an administrator comments the project
    Then an email notification about that comment is sent to the project creator
