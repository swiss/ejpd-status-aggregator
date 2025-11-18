package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import ch.ejpd.servicecheck.actuatoraggregation.health.DefaultApplicationStatusHealthAggregator;
import ch.ejpd.servicecheck.actuatoraggregation.health.DefaultServiceStatusAggregator;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.ejpd.servicecheck.actuatoraggregation.DicoveryClientMockBuilder.discoveryClientWith;
import static ch.ejpd.servicecheck.actuatoraggregation.ServiceInstanceMockBuilder.instance;
import static ch.ejpd.servicecheck.actuatoraggregation.health.StatusList.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("LawOfDemeter")
public class ServiceHealthAggregationConfigurationTest {


    private static final Map<String,Object> HEALTH_CHECK_RESULT_UP = new HashMap<>();
    private static final Map<String,Object> HEALTH_CHECK_RESULT_DOWN = new HashMap<>();

    static {
        HEALTH_CHECK_RESULT_DOWN.put("status", "DOWN");
        HEALTH_CHECK_RESULT_UP.put("status", "UP");
    }


    @Test
    void twoServicesOneInstanceEachAllUp() {

        final ServiceInstance instance1 = instance("service1", "instanceA");
        final ServiceInstance instance2 = instance("service2", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance1, instance2);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1", "service2"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().up()
                .withDetail("service2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void twoServicesOneInstanceEachAllUp_withCustomApplicationStateAggregator() {

        final ServiceInstance instance1 = instance("service1", "instanceA");
        final ServiceInstance instance2 = instance("service2", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance1, instance2);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1", "service2"),
                discoveryClientMock, healthInfoClientMock, (map) -> new Status("CustomStatus"), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().status("CustomStatus")
                .withDetail("service2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()).build())
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()).build())
                .build();
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void servicesExpected_butNotPresentInRegistry_areMarkedAsDown() {

        final ServiceInstance instance = instance("service1", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1", "service2"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().down()
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .withDetail("service2", new Health.Builder().down()
                        .withDetail("error", "There is no available instance of service service2 in the service registry!")
                        .build())
                .build();
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void overallStateIsUp_ifAtLeasOneInstancePerService_isUp() {

        final ServiceInstance instanceA = instance("service1", "instanceA");
        final ServiceInstance instanceB = instance("service1", "instanceB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instanceA, instanceB);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceA))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceB))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().up()
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void overallStateIsDown_ifAllInstancesForAService_areDown() {

        final ServiceInstance instance1A = instance("service1", "instanceA");
        final ServiceInstance instance1B = instance("service1", "instanceB");
        final ServiceInstance instance2A = instance("service2", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance1A, instance1B, instance2A);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1A))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1B))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2A))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1", "service2"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().down()
                .withDetail("service1", new Health.Builder().down()
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .withDetail("instance-1", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .build())
                .withDetail("service2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void servicesNotIn_neededServices_areNotDisplayed() {

        final ServiceInstance instance1A = instance("service1", "instanceA");
        final ServiceInstance instance2A = instance("service2", "instanceA");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance1A, instance2A);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1A))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2A))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service2"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().up()
                .withDetail("service2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void servicesWithCustomThresholds() {

        final ServiceInstance instance1A = instance("service1", "instanceA");
        final ServiceInstance instance1B = instance("service1", "instanceB");
        final ServiceInstance instance1C = instance("service1", "instanceC");
        final ServiceInstance instance2A = instance("service2", "instanceA");
        final ServiceInstance instance2B = instance("service2", "instanceB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance1A, instance1B, instance1C, instance2A, instance2B);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1A))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1B))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1C))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2A))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance2B))).thenReturn(HEALTH_CHECK_RESULT_UP);

        final HealthAggregatorProperties properties = createProperties("service1", "service2");

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", properties,
                        discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(),
                        (service, status) -> service.getServiceName().equalsIgnoreCase("service1") ? WARNING : OUT_OF_SERVICE)
                .health();

        var expected = new Health.Builder().status(OUT_OF_SERVICE)
                .withDetail("service1", new Health.Builder().status(WARNING)
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .withDetail("instance-2", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceC:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .build())
                .withDetail("service2", new Health.Builder().status(OUT_OF_SERVICE)
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service2instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void serviceWithCustomThreshold_noAvailableInstance() {

        final DiscoveryClient discoveryClientMock = discoveryClientWith();
        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        final HealthAggregatorProperties properties = createProperties("service1");

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", properties,
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), (service, status) -> Status.UP).health();

        var expected = new Health.Builder().up()
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("error", "There is no available instance of service service1 in the service registry!")
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void twoServiceInstancesInSeparateZone_configuredToFetchBoth() {

        final ServiceInstance instance = instance("service1", "instanceA", "zoneA");
        final ServiceInstance instance1 = instance("service1", "instanceB", "zoneB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instance, instance1);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instance1))).thenReturn(HEALTH_CHECK_RESULT_UP);

        final HealthAggregatorProperties healthAggregatorProperties = new HealthAggregatorProperties() {
            @Override
            public boolean isIgnoreOtherRegistryZones() {
                return false;
            }
        };
        healthAggregatorProperties.setNeededServices(createServices("service1"));

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("zoneA", healthAggregatorProperties,
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().up()
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void twoInstance_oneUp_oneDown_resultsInWarning() {

        final ServiceInstance instanceA = instance("service1", "instanceA");
        final ServiceInstance instanceB = instance("service1", "instanceB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instanceA, instanceB);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceA))).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceB))).thenReturn(HEALTH_CHECK_RESULT_UP);

        final HealthAggregatorProperties healthAggregatorProperties = new HealthAggregatorProperties() {
            @Override
            public boolean isIgnoreOtherRegistryZones() {
                return false;
            }
        };
        healthAggregatorProperties.setNeededServices(createServices("service1"));
        healthAggregatorProperties.setNeededServices(Map.of("service1", 2));

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("zoneA", healthAggregatorProperties,
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().status(WARNING)
                .withDetail("service1", new Health.Builder().status(WARNING)
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceB:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(WARNING);
        assertThat(health).isEqualTo(expected);
    }


    @Test
    void twoServiceInstancesInSeparateZone_onlyOwnZoneIsChecked() {

        final ServiceInstance instanceA = instance("service1", "instanceA", "zoneA");
        final ServiceInstance instanceB = instance("service1", "instanceB", "zoneB");
        final DiscoveryClient discoveryClientMock = discoveryClientWith(instanceA, instanceB);

        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceA))).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap(eq(instanceB))).thenReturn(HEALTH_CHECK_RESULT_UP);

        var health = new ServiceHealthAggregationConfiguration().aggregatedServices("zoneA", createProperties("service1"),
                discoveryClientMock, healthInfoClientMock, DefaultApplicationStatusHealthAggregator.create(), new DefaultServiceStatusAggregator()).health();

        var expected = new Health.Builder().up()
                .withDetail("service1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", URI.create("http://service1instanceA:8081/actuator/health"))
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build();

        assertThat(health.getStatus()).isEqualTo(UP);
        assertThat(health).isEqualTo(expected);
    }


    private HealthAggregatorProperties createProperties(String... services) {
        final HealthAggregatorProperties healthAggregatorProperties = new HealthAggregatorProperties();
        healthAggregatorProperties.setNeededServices(createServices(services));
        return healthAggregatorProperties;
    }

    private Map<String,Integer> createServices(String... serviceNames) {
        return Stream.of(serviceNames).collect(Collectors.toMap(Function.identity(), s -> 1));
    }

}
