package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;


public class DefaultApplicationStatusHealthAggregator implements ApplicationStatusHealthAggregator {

    private final List<String> sortedStatus;

    DefaultApplicationStatusHealthAggregator(List<String> sortedStatus) {
        this.sortedStatus = sortedStatus;
    }

    public static DefaultApplicationStatusHealthAggregator create() {
        return new DefaultApplicationStatusHealthAggregator(asList("DOWN", "OUT_OF_SERVICE", "WARNING", "UP", "UNKNOWN"));
    }

    public static DefaultApplicationStatusHealthAggregator createFrom(List<String> sortedStatus) {
        return (sortedStatus == null || sortedStatus.isEmpty())
                ? create()
                : new DefaultApplicationStatusHealthAggregator(sortedStatus);
    }

    @Override
    public Status aggregateApplicationStatus(Set<Status> statuses) {
        return new SimpleStatusAggregator(sortedStatus).getAggregateStatus(statuses);
    }
}
