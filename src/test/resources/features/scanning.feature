Feature: Scanning websites for issues
  As a user
  I want to scan websites for issues
  So that I can gather insights on potential problems or improvements

  Scenario: Scan a website with the "SeoScanner" and verify a report is created
    Given an asset "example.com" exists in the database
    When I send a POST request to "/api/scan" with body:
      """
      {
        "domains": ["example.com"],
        "scanners": ["SeoScanner"]
      }
      """
    And I send a GET request to "/api/reports" to retrieve the scan results
    Then I should see a report for "example.com"