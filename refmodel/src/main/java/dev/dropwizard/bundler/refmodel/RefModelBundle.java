package dev.dropwizard.bundler.refmodel;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Module;
import dev.dropwizard.bundler.features.ReflectionsBundle;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.reflections.ReflectionUtils;
import org.reflections.scanners.MemberUsageScanner;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.FluentIterable.*;

/**
 *
 */
public class RefModelBundle implements ConfiguredBundle<Configuration>, Module {

    @Inject ReflectionsBundle reflectionsBundle;

    private Set<String> modelKeys;
    private Map<Class<? extends Annotation>, Map<Object, Class<?>>> modelsMap = new HashMap<>();

    @Override
    public void configure(Binder binder) {
        binder.bind(IdResolver.class).to(IdResolver.Composite.class);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
    }

    public void addModels(Class<? extends Annotation> domain, Set<Class<?>> modelClasses) {
        Map<Object, Class<?>> map = new HashMap<>();
        for (Class<?> modelClass : modelClasses) {
            map.put(modelClass.getSimpleName(), modelClass);
        }
        modelsMap.put(domain, map);
    }

    public Class<?> getModel(Class<? extends Annotation> domain, String model) {
        return modelsMap.get(domain).get(model);
    }

    public Set<String> getRefModelKeys(final Class<?> modelClass) {
        return Sets.filter(getModelKeys(), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                String modelClassName = input.substring(0, input.indexOf("#"));
                return modelClassName.equals(modelClass.getName());
            }
        });
    }

    public Multimap<Class<?>, String> getPropertyMap() {
        try {
            Multimap<Class<?>, String> propertyMap = HashMultimap.create();
            for (String modelKey : getModelKeys()) {
                String className = modelKey.substring(0, modelKey.indexOf("#"));
                String property = modelKey.substring(modelKey.indexOf("#") + 1);
                propertyMap.put(Class.forName(className), property);
            }
            return propertyMap;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getModelKeys() {
        if (modelKeys == null) {
            Set<String> refModelKeys = reflectionsBundle.get().getStore().get(MemberUsageScanner.class.getSimpleName()).keySet();
            modelKeys = from(refModelKeys).
                    filter(new Predicate<String>() {
                        @Override
                        public boolean apply(@Nullable String input) {
                            return !input.contains("$VALUES");
                        }
                    }).
                    transform(new Function<String, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable String input) {
                            return getModelKey(input);
                        }
                    }).toSet();
        }
        return modelKeys;
    }

    private String getModelKey(String refModelKey) {
        String refModelName = refModelKey.substring(0, refModelKey.lastIndexOf("."));
        String refModelProperty = refModelKey.substring(refModelKey.lastIndexOf(".") + 1);
        String objName = refModelName.substring(refModelName.lastIndexOf("$") + 1);
        Class<?> objClass = ReflectionUtils.forName(refModelName);
        RefPackage annotation = objClass.getAnnotation(RefPackage.class);
        String refPackage = annotation.value();
        return refPackage + "." + objName + "#" + refModelProperty;
    }
}
