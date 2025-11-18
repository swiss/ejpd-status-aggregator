package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;

@FunctionalInterface
public interface HealthAggregatorRestTemplateBuilderConfigurer {
    RestTemplateBuilder configure(RestTemplateBuilder restTemplateBuilder);
}
