package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import ch.ejpd.servicecheck.actuatoraggregation.health.CompositeServicesHealthIndicator;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import ch.ejpd.servicecheck.applicationcheckproperties.configuration.EnableServiceCheckFileWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@SuppressWarnings("SpringFacetCodeInspection")
@Configuration
@EnableConfigurationProperties(HealthAggregatorProperties.class)
@EnableDiscoveryClient
@EnableServiceCheckFileWriter
class ServiceHealthAggregationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    HealthInfoClient healthInfoClient() {
        return new HealthInfoClient();
    }

    @Bean
    @ConditionalOnMissingBean
    CompositeServicesHealthIndicator aggregatedServices(
            @Value("${healthaggregator.registryzone:${eureka.instance.metadataMap.zone:#{null}}}") String myRegistryZone,
            HealthAggregatorProperties properties, @SuppressWarnings("SpringJavaAutowiringInspection") DiscoveryClient discoveryClient, HealthInfoClient healthInfoClient, OrderedHealthAggregator orderedHealthAggregator) {
        final Map<String,InstancesThreshold> stringInstancesThresholdMap = properties.thresholdMap();
        return new CompositeServicesHealthIndicator(myRegistryZone, properties.isIgnoreOtherRegistryZones(), properties.getNeededServices(), discoveryClient, healthInfoClient, stringInstancesThresholdMap, orderedHealthAggregator);
    }

}
