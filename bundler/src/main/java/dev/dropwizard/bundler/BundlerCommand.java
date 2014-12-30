package dev.dropwizard.bundler;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.hubspot.dropwizard.guice.GuiceBundle;
import dev.dropwizard.bundler.features.BundlesOrder;
import dev.dropwizard.bundler.features.DynamicConfigHelper;
import dev.dropwizard.bundler.features.PartialConfigFactory;
import dev.dropwizard.bundler.features.ReflectionsBundle;
import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.cli.ServerCommandExtended;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.setup.EnvironmentExtended;
import net.sourceforge.argparse4j.inf.Namespace;
import org.reflections.Reflections;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.FluentIterable.from;

/**
*
*/
public class BundlerCommand<T extends io.dropwizard.Configuration> extends ServerCommandExtended<T> implements ConfiguredBundle<T> {

    private PartialConfigFactory configFactory;
    private ReflectionsBundle reflectionsBundle;
    private Injector injector;

    public BundlerCommand(Application<T> application) {
        super(application, "bundler", "Runs the dw-bundler application as an HTTP server");
    }

    //ConfiguredCommand
    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
        //noinspection unchecked
        bootstrap.setConfigurationFactoryFactory(configFactory = new PartialConfigFactory(bootstrap));
        super.run(bootstrap, namespace);
    }

    //EnvironmentCommand
    @Override
    protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) throws Exception {
        List<Object> instances = initializeDynamicBootstrap(bootstrap, namespace);
        final Environment environment = buildEnvironment(bootstrap, reflectionsBundle.get(), injector);
        configuration.getMetricsFactory().configure(environment.lifecycle(), bootstrap.getMetricRegistry());
        runDynamicBootstrap(instances, bootstrap, environment);
        application.run(configuration, environment);
        //start server
        run(environment, namespace, configuration);
    }

    //ConfiguredBundle
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.addCommand(this);
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
    }

    //DynamicBootstrap
    private List<Object> initializeDynamicBootstrap(Bootstrap<T> bootstrap, Namespace namespace) {
        try {
            //scan
            reflectionsBundle = new ReflectionsBundle();
            ReflectionsBundle.Configuration refConf = configFactory.buildConfig(reflectionsBundle.getClass(), bootstrap);
            reflectionsBundle.run(refConf, null);

            //discover
            List<Object> instances = getInstances(reflectionsBundle.getDiscoveredTypes());
            instances.add(reflectionsBundle);
            instances.add(reflectionsBundle.get());

            //guice
            ImmutableList<Module> mods = ImmutableList.<Module>builder().
                    addAll(from(instances).filter(Module.class)).
                    add(new BootstrapInstancesModule(instances)).
                    build();
            GuiceBundle<T> guiceBundle = buildGuiceBundle(mods);
            Thread hook = null;
            try {
                //delay System.exit so that log will be flushed
                Runtime.getRuntime().addShutdownHook(hook = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }));
                bootstrap.addBundle(guiceBundle);
            } finally {
                if (hook != null) Runtime.getRuntime().removeShutdownHook(hook);
            }
            injector = guiceBundle.getInjector();

            //add discovered bundles
            instances = from(instances).toSortedList(injector.getInstance(BundlesOrder.class));
            for (Object object : instances) {
                if (object instanceof Bundle) {
                    injector.injectMembers(object);
                    bootstrap.addBundle((Bundle) object);
                } else if (object instanceof ConfiguredBundle) {
                    injector.injectMembers(object);
                    //noinspection unchecked
                    bootstrap.addBundle((ConfiguredBundle) object);
                }
            }
            return instances;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<Object> getInstances(Collection<Class<?>> types) {
        return Lists.newArrayList(from(types).transform(new Function<Class<?>, Object>() {
            @Nullable
            @Override
            public Object apply(Class<?> input) {
                try {
                    return input.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Could not instantiate " + input, e);
                }
            }
        }));
    }

    private void runDynamicBootstrap(List<Object> instances, Bootstrap<T> bootstrap, Environment environment) throws Exception {
        List<Bundle> bundles = Lists.newArrayList(DynamicConfigHelper.<List<Bundle>>getInvoke(bootstrap, "bundles"));
        List<ConfiguredBundle> configuredBundles = Lists.newArrayList(DynamicConfigHelper.<List<ConfiguredBundle>>getInvoke(bootstrap, "configuredBundles"));

        Set<Object> all = from(ImmutableSet.builder().addAll(instances).addAll(bundles).addAll(configuredBundles).build()).
                toSortedSet(injector.getInstance(BundlesOrder.class));

        for (Object object : all) {
            if (object instanceof Bundle) {
                ((Bundle) object).run(environment);
            } else if (object instanceof ConfiguredBundle) {
                Object bundleConf = configFactory.buildConfig(object.getClass(), bootstrap);
                //noinspection unchecked
                ((ConfiguredBundle) object).run(bundleConf, environment);
            }
        }
    }

    //DynamicEnvironment
    private EnvironmentExtended buildEnvironment(Bootstrap<T> bootstrap, Reflections reflections, Injector injector) {
        return new EnvironmentExtended(bootstrap, reflections, injector);
    }

    private GuiceBundle<T> buildGuiceBundle(Collection<Module> modules) {
        GuiceBundle.Builder<T> builder = new GuiceBundle.Builder<>();
        for (Module module : modules) builder.addModule(module);

        //noinspection unchecked
        Class<T> aClass = (Class<T>) getClass().getTypeParameters()[0].getBounds()[0]; //todo do it differently
        builder.setConfigClass(aClass);
        return builder.build();
    }

    public static class BootstrapInstancesModule implements Module {
        private final Iterable<?> instances;

        public BootstrapInstancesModule(Iterable<?> instances) {
            this.instances = instances;
        }

        @Override
        public void configure(Binder binder) {
            for (Object instance : instances) {
                //noinspection unchecked
                binder.bind((Class) instance.getClass()).toInstance(instance);
            }
        }
    }
}
