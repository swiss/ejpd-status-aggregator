package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.HealthAggregatorProperties;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.ServiceInstanceURIResolver;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Map;

public class DefaultServiceInstanceHealthIndicator extends AbstractHealthIndicator {

    protected ServiceInstance serviceInstance;
    protected HealthInfoClient healthInfoClient;
    protected HealthAggregatorProperties.HttpServiceSettings serviceSettings;

    DefaultServiceInstanceHealthIndicator(ServiceInstance serviceInstance, HealthInfoClient healthInfoClient, HealthAggregatorProperties.HttpServiceSettings serviceSettings) {
        this.serviceInstance = serviceInstance;
        this.healthInfoClient = healthInfoClient;
        this.serviceSettings = serviceSettings;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        final Map<String,Object> forObject = healthInfoClient.getHealthInfoMap(serviceInstance);
        builder.status(forObject.get("status").toString()).withDetail("healthCheckUrl", ServiceInstanceURIResolver.getHealthUrl(serviceInstance))
                .withDetail("instanceHealth", forObject);
    }

}
