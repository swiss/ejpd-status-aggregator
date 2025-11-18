package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.Collections;

/**
 * HealthIndicator for a service which is not available in repository.
 */
class AbsentServiceHealthIndicator implements HealthIndicator, HealthContributor {

    private NeededService neededService;
    private ServiceStatusAggregator serviceStatusAggregator;

    AbsentServiceHealthIndicator(NeededService neededService, ServiceStatusAggregator serviceStatusAggregator) {
        this.neededService = neededService;
        this.serviceStatusAggregator = serviceStatusAggregator;
    }

    @Override
    public Health health() {
        Status status = serviceStatusAggregator.aggregateServiceStatus(neededService, Collections.emptySet());
        return new Health.Builder().status(status).withDetail("error", "There is no available instance of service " + neededService.getServiceName() + " in the service registry!").build();
    }
}
