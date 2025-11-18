package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.configuration.HealthAggregatorProperties;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static ch.ejpd.servicecheck.actuatoraggregation.DicoveryClientMockBuilder.discoveryClientWith;
import static ch.ejpd.servicecheck.actuatoraggregation.ServiceInstanceMockBuilder.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

public class CompositeServicesHealthIndicatorTest {


    private static HashMap<String, Object> statusUpMap = new HashMap<>();

    static {
        statusUpMap.put("status", "UP");
    }


    @Test
    void healthIsConsideredDown_ifDiscoveryClient_returnsNullForServices() {
        final DiscoveryClient discoveryClientMock = mock(DiscoveryClient.class);
        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();
        final CompositeServicesHealthIndicator compositeServicesHealthIndicator = new CompositeServicesHealthIndicator("zone1",
                true, Map.of("service1", 1), discoveryClientMock, mock(HealthInfoClient.class), httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator());

        final Health health = compositeServicesHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void healthIsConsideredDown_ifDiscoveryClient_returnsEmptyListForServices() {
        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();
        final DiscoveryClient discoveryClientMock = mock(DiscoveryClient.class);
        when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        final CompositeServicesHealthIndicator compositeServicesHealthIndicator = new CompositeServicesHealthIndicator("zone1", true,
                Map.of("service1", 1), discoveryClientMock, mock(HealthInfoClient.class), httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator());

        final Health health = compositeServicesHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }


    @Test
    void healthIsConsideredUp_forSingleServiceInstanceThatIsUp() {
        final ServiceInstance instance = instance("service1", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(
                instance
        );

        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();
        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance))).thenReturn(statusUpMap);

        final Health health = new CompositeServicesHealthIndicator("defaultZone", true, Map.of("service1", 1), discoveryClientMock, healthInfoClientMock, httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void exceptionThrown_whenTheServiceInstance_hasNoZoneInformation() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();

            final ServiceInstance instance = instance("service1", "instanceA", null);
            final DiscoveryClient discoveryClientMock
                    = discoveryClientWith(instance);

            final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
            when(healthInfoClientMock.getHealthInfoMap(instance)).thenReturn(statusUpMap);

            new CompositeServicesHealthIndicator("defaultZone", true, Map.of("service1", 1), discoveryClientMock, healthInfoClientMock, httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();
        });
    }


    @Test
    void compositeServicesHealthIndicator_throwsException_whenNoZoneInformationIsPresent() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();

            final DiscoveryClient discoveryClientMock
                    = discoveryClientWith(
                    instance("service1", "instanceA")
            );

            new CompositeServicesHealthIndicator(null, true, Map.of("service1", 1), discoveryClientMock, mock(HealthInfoClient.class), httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();
        });
    }


    @Test
    void healthFromOtherRegistryZone_isIgnored() {
        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();

        final ServiceInstance instance1 = instance("service1", "instanceA", "zoneA");
        final ServiceInstance instance2 = instance("service1", "instanceB", "zoneB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(
                instance1,
                instance2
        );

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1))).thenReturn(statusUpMap);

        new CompositeServicesHealthIndicator("zoneA", true, Map.of("service1", 1), discoveryClientMock, healthInfoClientMock, httpSettings, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        verify(healthInfoClientMock, times(1)).getHealthInfoMap(eq(instance1));
        verifyNoMoreInteractions(healthInfoClientMock);
    }


}
