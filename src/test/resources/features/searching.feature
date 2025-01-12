Feature: Searching for websites using Serper API
  As a user
  I want to search for websites and gather their information
  So that I can identify potential clients or leads

  Scenario: Search for websites by query
    Given I want to find websites related to "electric cars"
    When I perform a POST request to "/api/search" with the query "electric cars"
    Then I should receive a list of relevant websites
    And the results should include domains