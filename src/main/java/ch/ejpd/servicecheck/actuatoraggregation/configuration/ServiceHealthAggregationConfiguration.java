package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import ch.ejpd.servicecheck.actuatoraggregation.health.*;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.RestTemplateRegistry;
import ch.ejpd.servicecheck.applicationcheckproperties.configuration.EnableServiceCheckFileWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@SuppressWarnings({"SpringFacetCodeInspection", "SpringJavaInjectionPointsAutowiringInspection"})
@Configuration
@EnableConfigurationProperties(HealthAggregatorProperties.class)
@EnableDiscoveryClient
@EnableServiceCheckFileWriter
class ServiceHealthAggregationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    HealthInfoClient healthInfoClient(RestTemplateRegistry restTemplateRegistry) {
        return new HealthInfoClient(restTemplateRegistry);
    }


    @Bean
    @ConditionalOnMissingBean
    RestTemplateRegistry restTemplateRegistry(RestTemplateBuilder restTemplateBuilder, HealthAggregatorRestTemplateBuilderConfigurer healthAggregatorRestTemplateConfigurer, HealthAggregatorProperties healthAggregatorProperties) {
        var registry = new RestTemplateRegistry();
        for (String service : healthAggregatorProperties.getNeededServices().keySet()) {

            final HealthAggregatorProperties.HttpServiceSettings timeoutsForService = healthAggregatorProperties.getHttp().getTimeoutsForService(service);
            final Duration connectionTimeoutMilliseconds = Duration.ofMillis(timeoutsForService.getConnectionTimeoutMilliseconds());
            final Duration readTimeoutMilliseconds = Duration.ofMillis(timeoutsForService.getReadTimeoutMilliseconds());

            final RestTemplateBuilder configuredRestTemplateBuilder = healthAggregatorRestTemplateConfigurer.configure(restTemplateBuilder);
            final RestTemplate restTemplate = configuredRestTemplateBuilder
                    .setReadTimeout(readTimeoutMilliseconds)
                    .setConnectTimeout(connectionTimeoutMilliseconds)
                    .errorHandler(new ResponseErrorHandler() {
                        @Override
                        public boolean hasError(ClientHttpResponse response) {
                            return false;
                        }

                        @Override
                        public void handleError(ClientHttpResponse response) {

                        }
                    })
                    .build();
            registry.setRestTemplateForService(service, restTemplate);
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    HealthAggregatorRestTemplateBuilderConfigurer healthAggregatorRestTemplateBuilderConfigurer() {
        return (restTemplateBuilder) -> restTemplateBuilder;
    }

    @Bean
    @ConditionalOnMissingBean
    ApplicationStatusHealthAggregator applicationStatusHealthAggregator(
            @Value("${management.endpoint.health.status.order:#{null}}") List<String> statusOrder
    ) {
        return DefaultApplicationStatusHealthAggregator.createFrom(statusOrder);
    }

    @Bean
    @ConditionalOnMissingBean
    ServiceStatusAggregator serviceStatusAggregator() {
        return new DefaultServiceStatusAggregator();
    }


    @Bean
    @ConditionalOnMissingBean
    CompositeServicesHealthIndicator aggregatedServices(
            @Value("${healthaggregator.registryzone:${eureka.instance.metadataMap.zone:#{null}}}") String myRegistryZone,
            HealthAggregatorProperties properties,
            @SuppressWarnings("SpringJavaAutowiringInspection") DiscoveryClient discoveryClient,
            HealthInfoClient healthInfoClient,
            ApplicationStatusHealthAggregator applicationStatusHealthAggregator,
            ServiceStatusAggregator serviceStatusAggregator) {
        return new CompositeServicesHealthIndicator(myRegistryZone, properties.isIgnoreOtherRegistryZones(), properties.getNeededServices(), discoveryClient, healthInfoClient, properties.getHttp(), applicationStatusHealthAggregator, serviceStatusAggregator);
    }

}
