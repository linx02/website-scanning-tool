package com.leads.leadsgen.bdd;

import com.leads.leadsgen.service.HttpClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A test configuration that creates a mock HttpClient bean,
 * overriding the real HttpClientImpl bean in the test context.
 */
@TestConfiguration
public class HttpClientMockConfig {

    @Bean
    @Primary
    public HttpClient mockHttpClient() {
        return Mockito.mock(HttpClient.class);
    }
}