Feature: Modify project

Scenario: A user opens the modification view of a project
  Given a project is available
  And an admin publishs the created project
  And there is no financing round active
  When the project detail page of this project is requested
  And the project edit button is not existing and not enabled
  When a user is logged in
  And the project detail page of this project is requested again
  And the project edit button is indeed existing and indeed enabled
  And the user clicks the edit button
  Then he is located at the project edit page
  And the form input fields are initialized with the project's data.

Scenario: A user modifies a project
  Given a project is available
  And there is no financing round active
  And a user is logged in
  And he directly opens the project edit view
  When he adapts the project details
  And he submits the edit project form
  Then he is redirected to the project detail page containing updated project data

Scenario: While modifying a project a user displays the rendered markdown description preview
  Given a project is available
  And there is no financing round active
  And a user is logged in
  And he directly opens the project edit view
  When he adapts the project details
  And he clicks the preview button
  Then he sees the rendered description markdown instead of the textarea
  When he clicks the preview button again
  Then he sees the textarea with the markdown source again.

Scenario: A user intending to edit a project in financing round just sees a disabled edit button
  Given a project is available
  And an admin publishs the created project
  And there is a financing round active
  And a user is logged in
  When the project detail page of this project is requested
  Then the project edit button is indeed existing and not enabled

