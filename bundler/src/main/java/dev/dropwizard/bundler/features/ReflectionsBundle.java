package dev.dropwizard.bundler.features;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import dev.dropwizard.bundler.BundlerCommand;
import dev.dropwizard.bundler.util.ReflectionsHelper;
import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Predicates.*;
import static org.reflections.ReflectionUtils.withClassModifier;

/**
 *
 */
public class ReflectionsBundle implements ConfiguredBundle<ReflectionsBundle.Configuration> {
    private static final Logger log = LoggerFactory.getLogger(ReflectionsBundle.class);

    private Reflections reflections;
    private ImmutableSet<Class<?>> discoveredTypes;
    private Application<? extends io.dropwizard.Configuration> application;

    public static class Configuration {
        public Ref reflections = new Ref();

        public static class Ref {
            /** auto extend packages (Module -> AbstractModule, Bundle -> AssetsBundle) */
            public String[] extPackages = {"io.dropwizard", "com.google.inject", "javax.ws.rs",
                    "com.wordnik.swagger.annotations"};

            /** auto scan packages (dw bundler core) */
            public String[] corePackages = {"dev.dropwizard.bundler"};

            /** auto scan packages (app), defaults to application's package */
            public String[] basePackages;

            /** auto scan packages (model), defaults to application's package */
            public String[] modelPackages;

            /** usage scan packages (refmodel), defaults to basePackage.refmodel */
            @Nullable public String refPackage;

            public Class[] includeSubTypes = {Module.class, Bundle.class, ConfiguredBundle.class};

            public List<Class<?>> excludeSubTypes = Arrays.asList(ReflectionsBundle.class, BundlerCommand.class, BundlerCommand.BootstrapInstancesModule.class);

            public void applyDefaultsFromApplication(String basePackage) {
                if (basePackage != null) {
                    if (basePackages == null) basePackages = new String[]{basePackage};
                    if (modelPackages == null) modelPackages = new String[]{basePackage};
                    if (refPackage == null) refPackage = basePackage + ".refmodel";
                }
            }
        }
    }

    public Reflections get() {
        return reflections;
    }

    public ImmutableSet<Class<?>> getDiscoveredTypes() {
        return discoveredTypes;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        application = bootstrap.getApplication();
    }

    @Override
    public void run(Configuration conf, Environment env) throws Exception {
        if (reflections == null) {
            Configuration.Ref refConf = conf.reflections;
            refConf.applyDefaultsFromApplication(application.getClass().getPackage().getName());
            reflections = scanTypes(refConf);
            ReflectionsHelper.expandSupertypes(reflections, SubTypesScanner.class,
                    new String[][]{refConf.extPackages, refConf.corePackages, refConf.basePackages});
            scanUsages(refConf);
            discoverTypes(refConf);
        }
    }

    private Reflections scanTypes(Configuration.Ref refConf) {
        FilterBuilder filter = new FilterBuilder();
        for (String[] packages : new String[][]{refConf.extPackages, refConf.corePackages, refConf.basePackages}) {
            if (packages != null) {
                for (String prefix : packages) {
                    filter.add(new FilterBuilder.Include(FilterBuilder.prefix(prefix)));
                }
            }
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();
        for (String[] packages : Arrays.asList(refConf.basePackages, refConf.modelPackages, refConf.corePackages)) {
            if (packages != null) builder.forPackages(packages);
        }
        builder.setScanners(
                new SubTypesScanner().filterResultsBy(filter),
                new TypeAnnotationsScanner().filterResultsBy(filter));

        return new Reflections(builder);
    }

    private void scanUsages(Configuration.Ref refConf) {
        if (refConf.refPackage != null) {
            reflections.merge(
                    new Reflections(new ConfigurationBuilder().
                            forPackages(refConf.refPackage).
                            forPackages(refConf.basePackages).
                            setScanners(
                                    new MemberUsageScanner().
                                            filterResultsBy(new FilterBuilder().includePackage(refConf.refPackage)))));
        }
    }

    private void discoverTypes(Configuration.Ref refConf) {
        ImmutableSet.Builder<Class<?>> typesBuilder = ImmutableSet.<Class<?>>builder();
        for (Class<?> type : refConf.includeSubTypes) {
            typesBuilder.addAll(reflections.getSubTypesOf(type));
        }
        ImmutableSet<Class<?>> types = typesBuilder.build();

        discoveredTypes = FluentIterable.from(types).
                filter(notNull()).
                filter(not(in(refConf.excludeSubTypes))).
                filter(not(withClassModifier(Modifier.ABSTRACT))).
                filter(not(withClassModifier(Modifier.INTERFACE))).
                filter(withClassModifier(Modifier.PUBLIC)).
                toSet();

        for (Class o : discoveredTypes) {
            log.info("Discovered " + o.getSimpleName());
        }
    }

}
