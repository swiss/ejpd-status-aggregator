package ch.ejpd.servicecheck.actuatoraggregation;

import ch.ejpd.servicecheck.actuatoraggregation.restclient.HealthInfoClient;
import ch.ejpd.servicecheck.actuatoraggregation.sampleapp.HealthAggregatorSampleApp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.client.ServiceInstance;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest(classes = HealthAggregatorSampleApp.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class HealthAggregatorSampleTimeoutsTest {

    @SpyBean
    private HealthInfoClient healthInfoClient;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    void timeoutSettings_areAsConfigured() {
        healthEndpoint.health();
        verify(healthInfoClient, times(1)).getHealthInfoMap(argThat(serviceInstanceMatcher("BAR")));
        verify(healthInfoClient, times(1)).getHealthInfoMap(argThat(serviceInstanceMatcher("FOO")));
    }

    private ArgumentMatcher<ServiceInstance> serviceInstanceMatcher(String bar1) {
        return new ArgumentMatcher<>() {
            @Override
            public boolean matches(ServiceInstance argument) {
                return argument.getMetadata().get("health.path").contains(bar1);
            }
        };
    }

}
