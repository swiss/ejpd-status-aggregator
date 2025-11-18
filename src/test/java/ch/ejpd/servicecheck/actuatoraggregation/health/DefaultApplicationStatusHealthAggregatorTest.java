package ch.ejpd.servicecheck.actuatoraggregation.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.util.Set;

import static ch.ejpd.servicecheck.actuatoraggregation.health.StatusList.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DefaultApplicationStatusHealthAggregatorTest {

    @Test
    void warning_moreSevere_thanUp() {
        Status status = DefaultApplicationStatusHealthAggregator.create().aggregateApplicationStatus(Set.of(WARNING, UP));
        assertThat(status).isEqualTo(WARNING);
    }

    @Test
    void down_moreSevere_thanAllOthers() {

        Status status = DefaultApplicationStatusHealthAggregator.create().aggregateApplicationStatus(Set.of(UP, DOWN, WARNING, OUT_OF_SERVICE, UNKNOWN));
        assertThat(status).isEqualTo(DOWN);
    }

    @Test
    void custom_state_isIgnored() {
        Status status = DefaultApplicationStatusHealthAggregator.create().aggregateApplicationStatus(Set.of(new Status("SomeCustomStatus"), UP));
        assertThat(status).isEqualTo(UP);
    }

}
