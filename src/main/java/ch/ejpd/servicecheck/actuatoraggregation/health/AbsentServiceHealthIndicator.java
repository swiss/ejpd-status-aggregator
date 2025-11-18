package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.InstancesThreshold;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * HealthIndicator for a service which is not available in repository.
 */
class AbsentServiceHealthIndicator implements HealthIndicator {

    private String neededService;
    private InstancesThreshold instancesThreshold;

    AbsentServiceHealthIndicator(String neededService, InstancesThreshold instancesThreshold) {
        this.neededService = neededService;
        this.instancesThreshold = instancesThreshold;
    }

    @Override
    public Health health() {
        final Status status = instancesThreshold.evaluateForUpInstances(0);
        return new Health.Builder().status(status).withDetail("error", "There is no available instance of service "+neededService+" in the service registry!").build();
    }
}
