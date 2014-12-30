package dev.dropwizard.bundler.redis;

import com.google.common.base.Strings;
import com.google.inject.ImplementedBy;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import static dev.dropwizard.bundler.redis.RedisRefModelBundle.KEY_PREFIX;

@ImplementedBy(KeyResolver.Simple.class)
public interface KeyResolver {
    String getKey(Object object, Object property);
    String getKey(String simpleName, @Nullable String property);
    String getKey(String simpleName, @Nullable String property, String propertyValue);

    public static class Simple implements KeyResolver {
        @Inject @Named(KEY_PREFIX) protected String keyPrefix;
        @Inject MapperHelper mapperHelper;

        public String getKey(Object object, Object property) {
            return getKey(object.getClass().getSimpleName(), "" + property, mapperHelper.getPropertyValue(object, property));
        }

        @Override
        public String getKey(String simpleName, @Nullable String property) {
            return keyPrefix + ":" + simpleName + (!Strings.isNullOrEmpty(property) ? ("." + property) : "");
        }

        public String getKey(String simpleName, String property, String propertyValue) {
            return getKey(simpleName, property) + ":" + propertyValue.replace(' ', '_');
        }
    }

    public static class Lowercase extends Simple {
        @Override
        public String getKey(Object object, Object property) {
            return getKey(object.getClass().getSimpleName(), "" + property, mapperHelper.getPropertyValue(object, property).toLowerCase());
        }
    }
}
