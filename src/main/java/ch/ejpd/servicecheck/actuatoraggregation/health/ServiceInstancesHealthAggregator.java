package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.InstancesThreshold;
import org.springframework.boot.actuate.health.AbstractHealthAggregator;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

public class ServiceInstancesHealthAggregator extends AbstractHealthAggregator {

    private InstancesThreshold instancesThreshold;

    ServiceInstancesHealthAggregator(InstancesThreshold instancesThreshold) {
        this.instancesThreshold = instancesThreshold;
    }

    @Override
    protected Status aggregateStatus(List<Status> candidates) {
        final long count = candidates.stream().filter(status -> status.equals(Status.UP)).count();
        return instancesThreshold.evaluateForUpInstances((int) count);
    }
}
