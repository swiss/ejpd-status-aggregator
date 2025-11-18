package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.Status;

import java.util.Set;

@FunctionalInterface
public interface ApplicationStatusHealthAggregator {
    Status aggregateApplicationStatus(Set<Status> statuses);
}
