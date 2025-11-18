package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;

import java.util.Map;

public class EurekaServiceInstanceHealthIndicator extends AbstractHealthIndicator {

    private EurekaDiscoveryClient.EurekaServiceInstance serviceInstance;
    private HealthInfoClient healthInfoClient;

    public EurekaServiceInstanceHealthIndicator(ServiceInstance serviceInstance, HealthInfoClient healthInfoClient) {
        this.serviceInstance = (EurekaDiscoveryClient.EurekaServiceInstance) serviceInstance;
        this.healthInfoClient = healthInfoClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        final String healthCheckUrl = serviceInstance.getInstanceInfo().getHealthCheckUrl();
        final Map<String,Object> forObject = healthInfoClient.getHealthInfoMap(healthCheckUrl);
        builder.status(forObject.get("status").toString()).withDetail("healthCheckUrl", healthCheckUrl)
        .withDetail("instanceHealth", forObject);
    }


}
