package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import ch.ejpd.servicecheck.actuatoraggregation.DiscoveryClientMockBuilder;
import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
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
    public void twoServicesOneInstanceEachAllUp() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .add()
                .service("service2")
                .withInstance("instanceA")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service2instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone", createProperties("service1", "service2"), discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();

        assertThat(health, sameBeanAs(new Health.Builder().up()
                .withDetail("SERVICE2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service2instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .withDetail("SERVICE1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()).build())
                .build()
        ));
    }


    @Test
    public void servicesExpected_butNotPresentInRegistry_areMarkedAsDown() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service2instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone",createProperties("service1", "service2"), discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();

        assertThat(health, sameBeanAs(new Health.Builder().down()
                .withDetail("SERVICE1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .withDetail("SERVICE2", new Health.Builder().down()
                        .withDetail("error", "There is no available instance of service SERVICE2 in the service registry!")
                        .build())
                .build()
        ));
    }


    @Test
    public void overallStateIsUp_ifAtLeasOneInstancePerService_isUp() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .withInstance("instanceB")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone",createProperties("service1"), discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();


        assertThat(health, statusIs(Status.UP));
        assertThat(health, sameBeanAs(new Health.Builder().up()
                .withDetail("SERVICE1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()).withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceB/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()).build())
                .build())

        );
    }


    @Test
    public void overallStateIsDown_ifAllInstancesForAService_areDown() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .withInstance("instanceB")
                .add()
                .service("service2")
                .withInstance("instanceA")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service2instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone",createProperties("service1", "service2"), discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();


        Assert.assertThat(health, statusIs(Status.DOWN));
        assertThat(health, sameBeanAs(new Health.Builder().down()
                .withDetail("SERVICE1", new Health.Builder().down()
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()).withDetail("instance-1", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service1instanceB/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()).build())
                .withDetail("SERVICE2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service2instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()
                        ).build()

                ).build()));
    }



    @Test
    public void servicesNotIn_neededServices_areNotDisplayed() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .add()
                .service("service2")
                .withInstance("instanceA")
                .add()
                .build();


        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap("http://service2instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone",createProperties("service2"), discoveryClientMock, healthInfoClientMock, new OrderedHealthAggregator()).health();


        assertThat(health, statusIs(Status.UP));
        assertThat(health, sameBeanAs(new Health.Builder().up()
                .withDetail("SERVICE2", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service2instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()
                        ).build()

                ).build()));
    }


    @Test
    public void servicesWithCustomThresholds() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .withInstance("instanceB")
                .withInstance("instanceC")
                .add()
                .service("service2")
                .withInstance("instanceA")
                .withInstance("instanceB")
                .add()
                .build();


        final HealthInfoClient healthInfoClientMock = mock(HealthInfoClient.class);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(healthInfoClientMock.getHealthInfoMap("http://service1instanceC/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap("http://service2instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_DOWN);
        when(healthInfoClientMock.getHealthInfoMap("http://service2instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final HealthAggregatorProperties properties = createProperties("service1","service2");
        Map<Status,Integer> thresholdsService1 = new HashMap<>();
        thresholdsService1.put(new Status("WARNING"), 1);
        thresholdsService1.put(Status.UP, 2);
        final HashMap<Status,Integer> thresholdsService2 = new HashMap<>();
        thresholdsService2.put(Status.OUT_OF_SERVICE, 1);
        thresholdsService2.put(Status.UP, 2);
        Map<String,Map<Status,Integer>> thresholdMaps = new HashMap<>();
        thresholdMaps.put("service1", thresholdsService1);
        thresholdMaps.put("service2", thresholdsService2);
        properties.setThresholds(thresholdMaps);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("defaultZone",properties, discoveryClientMock, healthInfoClientMock, new OrderedHealthAggregator()).health();


        assertThat(health, sameBeanAs(new Health.Builder().status(Status.OUT_OF_SERVICE)
                .withDetail("SERVICE1", new Health.Builder().status("WARNING")
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()
                        )
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceB/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()
                        )
                        .withDetail("instance-2", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service1instanceC/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()
                        ).build()

                )
                .withDetail("SERVICE2", new Health.Builder().status(Status.OUT_OF_SERVICE)
                        .withDetail("instance-0", new Health.Builder().down()
                                .withDetail("healthCheckUrl", "http://service2instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_DOWN)
                                .build()
                        )
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service2instanceB/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()
                        ).build()

                ).build()));
    }


    @Test
    public void twoServiceInstancesInSeparateZone_configuredToFetchBoth() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .inZone("zoneA")
                .withInstance("instanceB")
                .inZone("zoneB")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final HealthAggregatorProperties healthAggregatorProperties = new HealthAggregatorProperties() {
            @Override
            public boolean isIgnoreOtherRegistryZones() {
                return false;
            }
        };
        healthAggregatorProperties.setNeededServices(Arrays.asList("service1"));
        final HealthAggregatorProperties properties = healthAggregatorProperties;
        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("zoneA", properties, discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();


        assertThat(health, sameBeanAs(new Health.Builder().up()
                .withDetail("SERVICE1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .withDetail("instance-1", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceB/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build()).build())
                .build()
        ));
    }


    @Test
    public void twoServiceInstancesInSeparateZone_onlyOwnZoneIsChecked() {

        final DiscoveryClient discoveryClientMock = new DiscoveryClientMockBuilder()
                .service("service1")
                .withInstance("instanceA")
                .inZone("zoneA")
                .withInstance("instanceB")
                .inZone("zoneB")
                .add()
                .build();


        final HealthInfoClient mhealthInfoClientMock = mock(HealthInfoClient.class);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceA/health")).thenReturn(HEALTH_CHECK_RESULT_UP);
        when(mhealthInfoClientMock.getHealthInfoMap("http://service1instanceB/health")).thenReturn(HEALTH_CHECK_RESULT_UP);

        final Health health = new ServiceHealthAggregationConfiguration().aggregatedServices("zoneA", createProperties("service1"), discoveryClientMock, mhealthInfoClientMock, new OrderedHealthAggregator()).health();


        assertThat(health, sameBeanAs(new Health.Builder().up()
                .withDetail("SERVICE1", new Health.Builder().up()
                        .withDetail("instance-0", new Health.Builder().up()
                                .withDetail("healthCheckUrl", "http://service1instanceA/health")
                                .withDetail("instanceHealth", HEALTH_CHECK_RESULT_UP)
                                .build())
                        .build())
                .build()
        ));
    }




    private HealthAggregatorProperties createProperties(String... services) {
        final HealthAggregatorProperties healthAggregatorProperties = new HealthAggregatorProperties();
        healthAggregatorProperties.setNeededServices(Arrays.asList(services));
        return healthAggregatorProperties;
    }




    private Matcher<Health> statusIs(Status status) {
        return new TypeSafeMatcher<Health>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Expected status to be up");
            }

            @Override
            protected boolean matchesSafely(Health item) {
                return item.getStatus().equals(status);
            }

        };



    }


}