package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.springframework.boot.actuate.health.Status;

import java.util.Set;

import static ch.ejpd.servicecheck.actuatoraggregation.health.StatusList.*;

public class DefaultServiceStatusAggregator implements ServiceStatusAggregator {

    @Override
    public Status aggregateServiceStatus(NeededService neededService, Set<Status> status) {
        int needed = neededService.getNeededInstances();
        long upInstances = status.stream().filter(UP::equals).count();
        long downInstances = status.stream().filter(StatusList.DOWN::equals).count();
        if (upInstances >= needed) {
            return UP;
        }
        if (downInstances >= needed || status.isEmpty()) {
            return DOWN;
        }
        return WARNING;
    }
}
