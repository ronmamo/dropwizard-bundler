package dev.dropwizard.bundler.features;

import com.google.inject.Injector;
import com.hubspot.dropwizard.guice.InjectableHealthCheck;
import com.sun.jersey.spi.inject.InjectableProvider;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.setup.EnvironmentExtended;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Set;

/**
 * Based on https://github.com/HubSpot/dropwizard-guice/blob/master/src/main/java/com/hubspot/dropwizard/guice/AutoConfig.java
 */
public class AutoConfigExtended<T> implements ConfiguredBundle<T> {
    final Logger logger = LoggerFactory.getLogger(AutoConfigExtended.class);

    private Reflections reflections;

    //changed: get Reflections and Injector instance at constructor
    //public AutoConfig(String... basePackages) {
//    public AutoConfigExtended(Reflections reflections, Injector injector) {
//        this.reflections = reflections;
//        this.injector = injector;
//    }

    @Override
    //changed: signature to match ConfiguredBundle
    //public void initialize(Bootstrap<?> bootstrap, Injector injector) {
    public void initialize(Bootstrap<?> bootstrap) {
        //removed:
        //addBundles(bootstrap, injector);
    }

    @Override
    //changed: signature to match ConfiguredBundle
    //public void run(Environment environment, Injector injector) {
    public void run(T configuration, Environment env) throws Exception {
        EnvironmentExtended environment = (EnvironmentExtended) env;
        Injector injector = environment.injector();
        reflections = environment.reflections();
        addHealthChecks(environment, injector);
        addProviders(environment, injector);
        addInjectableProviders(environment, injector);
        addResources(environment, injector);
        addTasks(environment, injector);
        addManaged(environment, injector);
    }

    private void addManaged(Environment environment, Injector injector) {
        Set<Class<? extends Managed>> managedClasses = reflections
                .getSubTypesOf(Managed.class);
        for (Class<? extends Managed> managed : managedClasses) {
            environment.lifecycle().manage(injector.getInstance(managed));
            logger.info("Added managed: {}", managed.getSimpleName());
        }
    }

    private void addTasks(Environment environment, Injector injector) {
        Set<Class<? extends Task>> taskClasses = reflections
                .getSubTypesOf(Task.class);
        for (Class<? extends Task> task : taskClasses) {
            environment.admin().addTask(injector.getInstance(task));
            logger.info("Added task: {}", task.getSimpleName());
        }
    }

    private void addHealthChecks(Environment environment, Injector injector) {
        for (Class<? extends InjectableHealthCheck> healthCheck : reflections.getSubTypesOf(InjectableHealthCheck.class)) {
            InjectableHealthCheck instance = injector.getInstance(healthCheck);
            environment.healthChecks().register(instance.getName(), instance);
            logger.info("Added injectableHealthCheck: {}", healthCheck.getSimpleName());
        }
    }

    @SuppressWarnings("rawtypes")
    private void addInjectableProviders(Environment environment,
                                        Injector injector) {
        Set<Class<? extends InjectableProvider>> injectableProviders = reflections
                .getSubTypesOf(InjectableProvider.class);
        for (Class<? extends InjectableProvider> injectableProvider : injectableProviders) {
            environment.jersey().register(injectableProvider);
            logger.info("Added injectableProvider: {}", injectableProvider.getSimpleName());
        }
    }

    private void addProviders(Environment environment, Injector injector) {
        Set<Class<?>> providerClasses = reflections
                .getTypesAnnotatedWith(Provider.class);
        for (Class<?> provider : providerClasses) {
            environment.jersey().register(provider);
            logger.info("Added provider class: {}", provider.getSimpleName());
        }
    }

    private void addResources(Environment environment, Injector injector) {
        Set<Class<?>> resourceClasses = reflections
                .getTypesAnnotatedWith(Path.class);
        for (Class<?> resource : resourceClasses) {
            environment.jersey().register(resource);
            logger.info("Added resource class: {}", resource.getSimpleName());
        }
    }

    private void addBundles(Bootstrap<?> bootstrap, Injector injector) {
        Set<Class<? extends Bundle>> bundleClasses = reflections
                .getSubTypesOf(Bundle.class);
        for (Class<? extends Bundle> bundle : bundleClasses) {
            bootstrap.addBundle(injector.getInstance(bundle));
            logger.info("Added bundle class {} during bootstrap", bundle.getSimpleName());
        }
    }
}
