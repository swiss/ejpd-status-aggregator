package ch.ejpd.servicecheck.actuatoraggregation.health;


import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ch.ejpd.servicecheck.actuatoraggregation.health.StatusList.*;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;

public class ServiceInstancesHealthAggregatorTest {

    @Test
    void statusIsUp_if100PercentOfNeededServicesAreUp() {
        ServiceInstancesHealthAggregator aggregator = new ServiceInstancesHealthAggregator(NeededService.create("FOO_SERVICE"), new DefaultServiceStatusAggregator());

        assertThat(aggregator.getAggregateStatus(singleton(UP))).isEqualTo(UP);
    }

    @Test
    void statusIsDown_if100PercentOfNeededServicesAreDown() {
        ServiceInstancesHealthAggregator aggregator = new ServiceInstancesHealthAggregator(NeededService.create("FOO_SERVICE"), new DefaultServiceStatusAggregator());

        assertThat(aggregator.getAggregateStatus(singleton(DOWN))).isEqualTo(DOWN);
    }


    @Test
    void statusIsWarning_ifNot100PercentOfNeededServicesAreUp() {
        ServiceInstancesHealthAggregator aggregator = new ServiceInstancesHealthAggregator(NeededService.create("FOO_SERVICE", 2), new DefaultServiceStatusAggregator());

        assertThat(aggregator.getAggregateStatus(Set.of(DOWN, UP))).isEqualTo(WARNING);
    }

    @Test
    void statusIsWarning_ifLessInstancesAreAvailable_thanNeeded() {
        ServiceInstancesHealthAggregator aggregator = new ServiceInstancesHealthAggregator(NeededService.create("FOO_SERVICE", 3), new DefaultServiceStatusAggregator());

        assertThat(aggregator.getAggregateStatus(new HashSet(List.of(UP, UP)))).isEqualTo(WARNING);
    }

    @Test
    void status_withCustomServiceStatusAggregator() {
        ServiceInstancesHealthAggregator aggregator = new ServiceInstancesHealthAggregator(NeededService.create("FOO_SERVICE", 42), (service, status) -> StatusList.OUT_OF_SERVICE);
        assertThat(aggregator.getAggregateStatus(new HashSet(List.of(UP, UP, OUT_OF_SERVICE, WARNING)))).isEqualTo(OUT_OF_SERVICE);
    }

}
