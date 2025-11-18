package ch.ejpd.servicecheck.actuatoraggregation.health;

import ch.ejpd.servicecheck.actuatoraggregation.DiscoveryClientMockBuilder;
import ch.ejpd.servicecheck.actuatoraggregation.configuration.InstancesThreshold;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CompositeServicesHealthIndicatorTest {


    private static HashMap<String,Object> STATUS_UP_MAP = new HashMap<>();
    static {
        STATUS_UP_MAP.put("status", "UP");
    }


    @Test
    public void healthIsConsideredDown_ifDiscoveryClient_returnsNullForServices() {
        final DiscoveryClient discoveryClientMock = mock(DiscoveryClient.class);
        final CompositeServicesHealthIndicator compositeServicesHealthIndicator = new CompositeServicesHealthIndicator("zone1",  true, singletonList("service1"), discoveryClientMock, mock(HealthInfoClient.class), createThresholdsMap(), new OrderedHealthAggregator());

        final Health health = compositeServicesHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    public void healthIsConsideredDown_ifDiscoveryClient_returnsEmptyListForServices() {
        final DiscoveryClient discoveryClientMock = mock(DiscoveryClient.class);
        when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        final CompositeServicesHealthIndicator compositeServicesHealthIndicator = new CompositeServicesHealthIndicator("zone1", true, singletonList("service1"), discoveryClientMock, mock(HealthInfoClient.class), createThresholdsMap(), new OrderedHealthAggregator());

        final Health health = compositeServicesHealthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }


    @Test
    public void healthIsConsideredUp_forSingleServiceInstanceThatIsUp() {
        final DiscoveryClient discoveryClientMock
                = new DiscoveryClientMockBuilder().service("service1").withInstance("instanceA").add().build();
        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(STATUS_UP_MAP);

        final Health health = new CompositeServicesHealthIndicator("defaultZone", true, singletonList("service1"), discoveryClientMock, healthInfoClientMock, createThresholdsMap(), new OrderedHealthAggregator()).health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionThrown_whenTheServiceInstance_hasNoZoneInformation() {
        final DiscoveryClient discoveryClientMock
                = new DiscoveryClientMockBuilder().service("service1").withInstance("instanceA").inZone(null).add().build();

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(STATUS_UP_MAP);

        new CompositeServicesHealthIndicator("defaultZone", true, singletonList("service1"), discoveryClientMock, healthInfoClientMock, createThresholdsMap(), new OrderedHealthAggregator()).health();
    }


    @Test(expected = IllegalArgumentException.class)
    public void compositeServicesHealthIndicator_throwsException_whenNoZoneInformationIsPresent() {
        final DiscoveryClient discoveryClientMock
                = new DiscoveryClientMockBuilder().service("service1").withInstance("instanceA").add().build();

        new CompositeServicesHealthIndicator(null, true, singletonList("service1"), discoveryClientMock, mock(HealthInfoClient.class), createThresholdsMap(), new OrderedHealthAggregator()).health();
    }


    @Test
    public void healthFromOtherRegistryZone_isIgnored() {
        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA").inZone("zoneA")
                .withInstance("instanceB").inZone("zoneB")
                .add().build();

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(STATUS_UP_MAP);

        final Health health = new CompositeServicesHealthIndicator("zoneA", true, singletonList("service1"), discoveryClientMock, healthInfoClientMock, createThresholdsMap(), new OrderedHealthAggregator()).health();

        verify(healthInfoClientMock, times(1)).getHealthInfoMap("http://service1instanceA/health");
        verifyNoMoreInteractions(healthInfoClientMock);
    }


    private Map<String,InstancesThreshold> createThresholdsMap() {
        return new HashMap<>();
    }

}