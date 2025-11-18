package ch.ejpd.servicecheck.actuatoraggregation.configuration;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

@Order(Ordered.LOWEST_PRECEDENCE - 100)
class ServiceHealthAggregationImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        return new String[]{
                ServiceHealthAggregationConfiguration.class.getCanonicalName()
        };

    }

}
