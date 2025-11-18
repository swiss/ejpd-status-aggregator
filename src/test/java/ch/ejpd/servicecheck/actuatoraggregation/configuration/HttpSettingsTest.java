package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpSettingsTest {

    @Test
    void defaultReadTimeout_is500ms() {
        final int readTimeoutMilliseconds = new HealthAggregatorProperties.HttpSettings().getTimeoutsForService("fooService").getReadTimeoutMilliseconds();

        assertThat(readTimeoutMilliseconds).isEqualTo(500);
    }

    @Test
    void defaultConnectionTimeout_is500ms() {
        final int connectionTimeoutMilliseconds = new HealthAggregatorProperties.HttpSettings().getTimeoutsForService("fooService").getConnectionTimeoutMilliseconds();

        assertThat(connectionTimeoutMilliseconds).isEqualTo(500);
    }


    @Test
    void overriddenConnectionTimeout_forFooService() {
        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();
        httpSettings.getServices().put("fooService", new HealthAggregatorProperties.HttpServiceSettings(100, 200));

        assertThat(httpSettings.getTimeoutsForService("fooService").getConnectionTimeoutMilliseconds()).isEqualTo(100);
        assertThat(httpSettings.getTimeoutsForService("fooService").getReadTimeoutMilliseconds()).isEqualTo(200);
        assertThat(httpSettings.getTimeoutsForService("barService").getConnectionTimeoutMilliseconds()).isEqualTo(500);
        assertThat(httpSettings.getTimeoutsForService("barService").getReadTimeoutMilliseconds()).isEqualTo(500);
    }


    @Test
    void overriddenDefaultSettings() {
        final HealthAggregatorProperties.HttpSettings httpSettings = new HealthAggregatorProperties.HttpSettings();
        httpSettings.setDefaultConnectionTimeoutMilliseconds(1);
        httpSettings.setDefaultReadTimeoutMilliseconds(2);

        httpSettings.getServices().put("fooService", new HealthAggregatorProperties.HttpServiceSettings(100, 200));

        assertThat(httpSettings.getTimeoutsForService("barService").getConnectionTimeoutMilliseconds()).isEqualTo(1);
        assertThat(httpSettings.getTimeoutsForService("barService").getReadTimeoutMilliseconds()).isEqualTo(2);
    }


}