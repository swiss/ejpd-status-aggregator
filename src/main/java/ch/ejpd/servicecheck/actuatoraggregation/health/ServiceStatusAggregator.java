package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.Status;

import java.util.Set;


/**
 * ServiceStatusAggregator erlaubt es Applikationen, das Default-Mapping des {@link ServiceInstancesHealthAggregator} zu übersteuern.
 */
@FunctionalInterface
public interface ServiceStatusAggregator {

    Status aggregateServiceStatus(NeededService neededService, Set<Status> status);
}
