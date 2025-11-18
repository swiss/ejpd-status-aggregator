package ch.ejpd.servicecheck.actuatoraggregation;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.HealthAggregatorRestTemplateBuilderConfigurer;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.RestTemplateRegistry;
import ch.ejpd.servicecheck.actuatoraggregation.sampleapp.HealthAggregatorSampleApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {HealthAggregatorSampleApp.class, RestTemplateConfigurationTest.Configuration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestTemplateConfigurationTest {

    public static final MyClientHttpRequestInterceptor MY_CLIENT_HTTP_REQUEST_INTERCEPTOR = new MyClientHttpRequestInterceptor();

    @Autowired
    private RestTemplateRegistry restTemplateRegistry;

    @Test
    void timeoutSettings_areAsConfigured() {
        final RestTemplate fooRestTemplate = restTemplateRegistry.getRestTemplateForService("FOO");

        assertThat(fooRestTemplate.getInterceptors().contains(MY_CLIENT_HTTP_REQUEST_INTERCEPTOR)).isTrue();
    }


    @TestConfiguration
    public static class Configuration {
        @Bean
        HealthAggregatorRestTemplateBuilderConfigurer healthAggregatorRestTemplateBuilderConfigurer() {
            return restTemplateBuilder -> restTemplateBuilder.interceptors(MY_CLIENT_HTTP_REQUEST_INTERCEPTOR);
        }
    }

    private static class MyClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            return null;
        }
    }
}
