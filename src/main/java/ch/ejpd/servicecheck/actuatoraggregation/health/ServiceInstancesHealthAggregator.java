package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

import java.util.Set;

public class ServiceInstancesHealthAggregator extends SimpleStatusAggregator {

    private NeededService neededService;
    private ServiceStatusAggregator serviceStatusAggregator;

    ServiceInstancesHealthAggregator(NeededService neededService, ServiceStatusAggregator serviceStatusAggregator) {
        this.neededService = neededService;
        this.serviceStatusAggregator = serviceStatusAggregator;
    }

    //    @Override
    //    protected Status aggregateStatus(List<Status> candidates) {
    //        return serviceStatusAggregator.aggregateServiceStatus(neededService, candidates);
    //    }

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        return serviceStatusAggregator.aggregateServiceStatus(neededService, statuses);
    }
}

