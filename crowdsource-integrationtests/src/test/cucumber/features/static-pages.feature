Feature: About us, FAQ and impress pages

  Scenario: A user visits the about us page
    Given a user is logged in
    When he visits the "Über uns" page
    Then he sees the text "Awesome!"
    And hee sees the text "Source-Hacker"

  Scenario: A user visits the FAQ page
    Given a user is logged in
    When he visits the "FAQ" page
    Then he sees the text "Frequently Asked Questions"
    And the content text "Ganz einfach." is not visible
    When he clicks on text "Wie reiche ich eine Idee ein?"
    Then the content text "Ganz einfach." is visible

  Scenario: A user visits the Imprint page
    Given a user is logged in
    When he visits the "Impressum" page
    Then he sees the text "Impressum"
