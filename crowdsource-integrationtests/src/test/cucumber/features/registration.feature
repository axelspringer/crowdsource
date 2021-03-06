Feature: Registration

  @ClearMailServer
  Scenario: A user registers a new account for the first time
    Given a user is on the registration page
    When the user enters his email address
    And the user accepts the terms of service
    And submits the registration form
    Then the user has 1 activation mails in his inbox with the last mail being a registration confirmation mail
    And a registration success message is shown that includes the user's email

  @ClearMailServer
  Scenario: A user tries to register an already registered but not yet activated account
    Given the user's email address is already registered but not activated
    Given a user is on the registration page
    When the user enters his email address
    And the user accepts the terms of service
    And submits the registration form
    Then the user has 2 activation mails in his inbox with the last mail being a registration confirmation mail
    And a registration success message is shown that includes the user's email

  @ClearMailServer
  Scenario: A user tries to register an already activated account
    Given the user's email address is already activated
    Given a user is on the registration page
    When the user enters his email address
    And the user accepts the terms of service
    And submits the registration form
    Then the validation error 'Die Email-Adresse wurde bereits aktiviert. Du kannst Dich mit Deinem Passwort bereits einloggen. Falls Du Dein Passwort vergessen hast, klick bitte hier.' is displayed on the email field

  @ClearMailServer
  Scenario: A user tries to register an already activated account with different case
    Given the user's email address is already activated
    Given a user is on the registration page
    When the user enters his email address in different case
    And the user accepts the terms of service
    And submits the registration form
    Then the validation error 'Die Email-Adresse wurde bereits aktiviert. Du kannst Dich mit Deinem Passwort bereits einloggen. Falls Du Dein Passwort vergessen hast, klick bitte hier.' is displayed on the email field


  @ClearMailServer
  Scenario: A user activates a freshly registered account
    Given the user's email address is already registered but not activated
    When the user clicks the email's activation link
    Then the activation form for the registration flow is displayed
    And the user enters a valid password twice on activation page
    And the user submits the activation form
    Then he is redirected to the index page
    And he can request an access token with his newly set password
    When he clicks on the New Project link in the navigation bar
    Then he is redirected to the project creation page

  @ClearMailServer
  Scenario: A user tries to active an already activated account
    Given the user's email address is already activated
    When the user clicks the email's activation link for the second time
    And the user enters a valid password twice on activation page
    And the user submits the activation form
    Then the validation error 'Dein Konto wurde bereits aktiviert.' is displayed

  @ClearMailServer
  Scenario: A user tries to activate a freshly registered account with a wrong activation token
    Given the user's email address is already registered but not activated
    When the user clicks the email's activation link
    When the user changes the activation token in the URL
    And the user enters a valid password twice on activation page
    And the user submits the activation form
    Then the validation error 'Der Aktivierungslink ist ungültig.' is displayed