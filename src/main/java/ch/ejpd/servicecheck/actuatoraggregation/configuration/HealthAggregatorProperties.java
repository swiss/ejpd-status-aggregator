package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@ConfigurationProperties(prefix = "healthaggregator")

public class HealthAggregatorProperties {

    private String registryzone = "${eureka.instance.metadataMap.zone}";

    private Map<String,Integer> neededServices = new HashMap<>();

    public Map<String,Integer> getNeededServices() {
        return neededServices;
    }

    private HttpSettings http = new HttpSettings();


    public HttpSettings getHttp() {
        return http;
    }

    public void setHttp(HttpSettings http) {
        this.http = http;
    }

    public void setNeededServices(Map<String,Integer> neededServices) {
        this.neededServices = neededServices;
    }


    public boolean isIgnoreOtherRegistryZones() {
        // Bewusst kein Property mit Getter- und Setter.
        // Kann einfach umgebaut werden wenn dieses Verhalten mal konfigurierbar sein soll!
        return true;
    }

    public String getRegistryzone() {
        return registryzone;
    }

    public void setRegistryzone(String registryzone) {
        this.registryzone = registryzone;
    }


    public static class HttpSettings {

        private Map<String,HttpServiceSettings> services = new HashMap<>();

        private int defaultConnectionTimeoutMilliseconds = 500;
        private int defaultReadTimeoutMilliseconds = 500;

        public int getDefaultConnectionTimeoutMilliseconds() {
            return defaultConnectionTimeoutMilliseconds;
        }

        public void setDefaultConnectionTimeoutMilliseconds(int defaultConnectionTimeoutMilliseconds) {
            this.defaultConnectionTimeoutMilliseconds = defaultConnectionTimeoutMilliseconds;
        }

        public int getDefaultReadTimeoutMilliseconds() {
            return defaultReadTimeoutMilliseconds;
        }

        public void setDefaultReadTimeoutMilliseconds(int defaultReadTimeoutMilliseconds) {
            this.defaultReadTimeoutMilliseconds = defaultReadTimeoutMilliseconds;
        }

        public Map<String,HttpServiceSettings> getServices() {
            return services;
        }

        public void setServices(Map<String,HttpServiceSettings> services) {
            this.services = services;
        }

        public HttpServiceSettings getTimeoutsForService(String serviceId) {
            final int readTimeout = this.getDefaultReadTimeoutMilliseconds();
            final int connectTimeout = this.getDefaultConnectionTimeoutMilliseconds();
            return services.getOrDefault(serviceId, new HttpServiceSettings(connectTimeout, readTimeout));
        }
    }

    public static class HttpServiceSettings {

        private int connectionTimeoutMilliseconds;
        private int readTimeoutMilliseconds;

        public HttpServiceSettings() {
            this(500, 500);
        }

        public HttpServiceSettings(int connectionTimeoutMilliseconds, int readTimeoutMilliseconds) {
            this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
            this.readTimeoutMilliseconds = readTimeoutMilliseconds;
        }

        public int getConnectionTimeoutMilliseconds() {
            return connectionTimeoutMilliseconds;
        }

        public void setConnectionTimeoutMilliseconds(int connectionTimeoutMilliseconds) {
            this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
        }

        public int getReadTimeoutMilliseconds() {
            return readTimeoutMilliseconds;
        }

        public void setReadTimeoutMilliseconds(int readTimeoutMilliseconds) {
            this.readTimeoutMilliseconds = readTimeoutMilliseconds;
        }

        @SuppressWarnings({"ObjectEquality", "NonFinalFieldReferenceInEquals"})
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HttpServiceSettings that = (HttpServiceSettings) o;
            return connectionTimeoutMilliseconds == that.connectionTimeoutMilliseconds &&
                    readTimeoutMilliseconds == that.readTimeoutMilliseconds;
        }

        @SuppressWarnings("NonFinalFieldReferencedInHashCode")
        @Override
        public int hashCode() {
            return Objects.hash(connectionTimeoutMilliseconds, readTimeoutMilliseconds);
        }

        @Override
        public String toString() {
            return "HttpServiceSettings{" +
                    "connectionTimeoutMilliseconds=" + connectionTimeoutMilliseconds +
                    ", readTimeoutMilliseconds=" + readTimeoutMilliseconds +
                    '}';
        }
    }
}
