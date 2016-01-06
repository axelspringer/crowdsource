Feature: Projects can have file attachments

  Scenario: A user adds a file attachment
    Given a project is available
    And a user is logged in
    And he directly opens the project edit view
    And he selects a file to be uploaded
    And the file selector is 'invisible'
    And the file information to be uploaded is displayed.
    When he clicks the file upload submit button
    Then an upload success message appeared
    And the file selector is 'visible' again
    And the attachment is visible in the attachments table
    And there is 'a' 'delete' button for the attachment
    And there is 'a' 'copy' button for the attachment
    And there is 'a' 'markdown' button for the attachment.

  Scenario: A user can see a file attachment in project detail view
    Given a project is available
    And the project has got a file attachment
    And a user is logged in
    And the project detail page of this project is requested
    Then the attachment is visible in the attachments table.
    And there is 'no' 'delete' button for the attachment
    And there is 'a' 'copy' button for the attachment
    And there is 'no' 'markdown' button for the attachment.

  Scenario: A user deletes an existing attachment in project edit view
    Given a project is available
    And the project has got a file attachment
    And a user is logged in
    And he directly opens the project edit view
    And the attachment is visible in the attachments table.
    When he clicks the file delete button
    Then there is no attachment table visible anymore.
