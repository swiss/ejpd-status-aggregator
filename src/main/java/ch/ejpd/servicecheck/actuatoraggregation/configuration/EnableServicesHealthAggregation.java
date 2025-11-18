package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ServiceHealthAggregationImportSelector.class)
public @interface EnableServicesHealthAggregation {
}
