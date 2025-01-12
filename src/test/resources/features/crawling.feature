Feature: Crawling websites to gather contact information

  Scenario: Crawl a website and store the asset
    Given the domain "example.com" returns valid HTML
    When I send a POST request to "/api/crawl" with domains ["example.com"]
    Then I should get a 200 response
    And when I GET "/api/assets"
    Then the response should contain an asset for "example.com"