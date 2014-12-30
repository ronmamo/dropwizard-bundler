package dev.dropwizard.bundler.features;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import dev.dropwizard.bundler.BundlerCommand;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.reflections.ReflectionUtils;
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
import java.util.ArrayList;
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

    public static class Configuration {
        public Ref reflections;

        public static class Ref {
            /** auto extend packages (Module -> AbstractModule, Bundle -> AssetsBundle) */
            public String[] extPackages = {"io.dropwizard", "com.google.inject", "javax.ws.rs",
                    "com.wordnik.swagger.annotations"};

            /** auto scan packages (dw bundler core) */
            public String[] corePackages = {"dev.dropwizard.bundler"};

            /** auto scan packages (app) */
            @NotEmpty public String[] basePackages;

            /** auto scan packages (model) */
            @NotEmpty public String[] modelPackages;

            /** usage scan packages (refmodel) */
            @Nullable public String refPackage;

            public String[][] includePackages = {extPackages, corePackages, basePackages};

            public Class[] includeSubTypes = {Module.class, Bundle.class, ConfiguredBundle.class};

            public List<Class<?>> excludeSubTypes = Arrays.asList(ReflectionsBundle.class, BundlerCommand.class, BundlerCommand.BootstrapInstancesModule.class);
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
    }

    @Override
    public void run(Configuration conf, Environment env) throws Exception {
        if (reflections == null) {
            Configuration.Ref refConf = conf.reflections;
            reflections = scanTypes(refConf);
            expandSupertypes(refConf);
            scanUsages(refConf);
            discoverTypes(refConf);
        }
    }

    private Reflections scanTypes(Configuration.Ref refConf) {
        FilterBuilder filter = new FilterBuilder();
        for (String[] packages : refConf.includePackages) {
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

    private void expandSupertypes(Configuration.Ref refConf) {
        Multimap<String, String> mmap = reflections.getStore().get(SubTypesScanner.class.getSimpleName());
        for (String key : Sets.newHashSet(mmap.keySet())) {
            if (!mmap.containsValue(key)) {
                for (String extPackage : refConf.extPackages) {
                    if (key.startsWith(extPackage)) {
                        expandSupertypes(mmap, key, ReflectionUtils.forName(key));
                    }
                }
            }
        }
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

    private void expandSupertypes(Multimap<String, String> mmap, String key, Class<?> type) {
        for (Class<?> supertype : supertypes(type)) {
            if (mmap.put(supertype.getName(), key)) {
                log.debug("expanded subtype {} -> {}", supertype.getName(), key);
            }
            expandSupertypes(mmap, supertype.getName(), supertype);
        }
    }

    private List<Class<?>> supertypes(Class<?> type) {
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        List<Class<?>> result = new ArrayList<>();
        if (superclass != Object.class && superclass != null) result.add(superclass);
        if (interfaces != null && interfaces.length > 0) result.addAll(Arrays.asList(interfaces));
        return result;
    }
}
