package io.dropwizard.setup;

import com.google.inject.Injector;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.reflections.Reflections;

/**
*
*/
public class EnvironmentExtended extends Environment {
    private final Reflections reflections;
    private final Injector injector;

    public EnvironmentExtended(Bootstrap<?> bootstrap, Reflections reflections, Injector injector) {
        super(bootstrap.getApplication().getName(), bootstrap.getObjectMapper(), bootstrap.getValidatorFactory().getValidator(), bootstrap.getMetricRegistry(), bootstrap.getClassLoader());
        this.reflections = reflections;
        this.injector = injector;
    }

    public Reflections reflections() {
        return reflections;
    }

    public Injector injector() {
        return injector;
    }
}
