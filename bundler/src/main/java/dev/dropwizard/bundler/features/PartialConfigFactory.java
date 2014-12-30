package dev.dropwizard.bundler.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.*;
import io.dropwizard.setup.Bootstrap;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
*
*/
public class PartialConfigFactory extends DefaultConfigurationFactoryFactory {
    private final Bootstrap<?> bootstrap;
    public String lastPath;

    public PartialConfigFactory(Bootstrap<?> bootstrap) {
        this.bootstrap = bootstrap;
    }

    public static Class<?> determineConfigurationClass(Class type) {
        for (Type genericInterfaces : type.getGenericInterfaces()) {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) genericInterfaces;
            if (ConfiguredBundle.class.isAssignableFrom(parameterizedType.getRawType())) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments[0] instanceof Class) {
                    return (Class<?>) actualTypeArguments[0];
                }
            }
        }
        try {
            return (Class<?>) ((ParameterizedTypeImpl) type.getGenericSuperclass()).getActualTypeArguments()[0];
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    private <C1> C1 parseBundleConfiguration(Class<C1> confClass, Bootstrap<?> bootstrap) {
        ConfigurationFactory<C1> configurationFactory = create(confClass,
                bootstrap.getValidatorFactory().getValidator(), bootstrap.getObjectMapper(), "dw");
        try {
            return configurationFactory.build(bootstrap.getConfigurationSourceProvider(), getLastPath());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse bundle configuration " + confClass, e);
        }
    }

    public <C1> C1 buildConfig(Class configuredBundle, Bootstrap<?> bootstrap) {
        Object bundleConf;
        Class confClass = determineConfigurationClass(configuredBundle);
        if (confClass != null) {
            bundleConf = parseBundleConfiguration(confClass, bootstrap);
        } else {
            bundleConf = parseBundleConfiguration(Configuration.class, bootstrap);
        }
        return (C1) bundleConf;
    }

    @Override
    public ConfigurationFactory create(final Class klass, Validator validator, ObjectMapper objectMapper, final String propertyPrefix) {
        //noinspection unchecked
        return new ConfigurationFactory(klass, validator, objectMapper, propertyPrefix) {
            @Override
            public Object build(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
                PartialConfigFactory.this.lastPath = path;
                return buildConfiguration(bootstrap, klass, path, propertyPrefix);
            }
        };
    }

    public <T> T buildConfiguration(Bootstrap bootstrap, Class<T> configClass, String superConfFile, @Nullable String propertyPrefix) throws IOException, ConfigurationValidationException {
        return buildConfiguration(configClass, superConfFile, bootstrap.getConfigurationSourceProvider(), bootstrap.getObjectMapper(), bootstrap.getValidatorFactory().getValidator(), propertyPrefix);
    }

    public <T> T buildConfiguration(Class<T> configClass, String path, ConfigurationSourceProvider sourceProvider,
                                    ObjectMapper objectMapper, Validator validator) throws IOException, ConfigurationValidationException {
        return buildConfiguration(configClass, path, sourceProvider, objectMapper, validator, null);
    }

    public <T> T buildConfiguration(Class<T> configClass, String path, ConfigurationSourceProvider sourceProvider,
                                     ObjectMapper objectMapper, Validator validator, @Nullable String propertyPrefix) throws IOException, ConfigurationValidationException {
        final JsonNode node = getJsonNode(path, sourceProvider, objectMapper);
        String prefix = propertyPrefix != null ? propertyPrefix.endsWith(".") ? propertyPrefix : propertyPrefix + '.' : null;

        return buildConfiguration(configClass, path, objectMapper, validator, node, prefix);
    }

    private JsonNode getJsonNode(String path, ConfigurationSourceProvider sourceProvider, ObjectMapper objectMapper) throws IOException {
        final JsonNode node;
        try (InputStream input = sourceProvider.open(path)) {
            YAMLParser parser = new YAMLFactory().createParser(input);
            node = objectMapper.readTree(parser);
            return node;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private <T> T buildConfiguration(Class<T> configClass, String path, ObjectMapper objectMapper,
                                     @Nullable Validator validator, JsonNode node, @Nullable String propertyPrefix) throws IOException, ConfigurationValidationException {
        if (propertyPrefix != null) {
            for (Map.Entry<Object, Object> pref : System.getProperties().entrySet()) {
                final String prefName = (String) pref.getKey();
                if (prefName.startsWith(propertyPrefix)) {
                    final String configName = prefName.substring(propertyPrefix.length());
                    addOverride(node, configName, System.getProperty(prefName));
                }
            }
        }

        removeUnusedProperties(configClass, node);
        T t = buildConfigurationClass(configClass, objectMapper, node);
        if (validator != null) {
            validate(t, validator, path);
        }
        return t;
    }

    private <T> void validate(T t, Validator validator, String path) throws ConfigurationValidationException {
        final Set<ConstraintViolation<T>> violations = validator.validate(t);
        if (!violations.isEmpty()) {
            throw new ConfigurationValidationException(path, violations);
        }
    }

    private <T> T buildConfigurationClass(Class<T> configClass, ObjectMapper objectMapper, JsonNode node) throws IOException {
        return objectMapper.readValue(new TreeTraversingParser(node), configClass);
    }

    private <T> void removeUnusedProperties(Class<T> configClass, JsonNode node) {
        Map<String, ? extends Member> setters = DynamicConfigHelper.getSetters(configClass);
        Set<String> rm = new HashSet<>();
        for (String o : Lists.newArrayList(node.fieldNames())) if (!setters.containsKey(o)) rm.add(o);
        for (String o : rm) ((ObjectNode) node).remove(o);
    }

    //from io.dropwizard.configuration.ConfigurationFactory
    private void addOverride(JsonNode root, String name, String value) {
        JsonNode node = root;
        final Iterable<String> split = Splitter.on('.').trimResults().split(name);
        final String[] parts = Iterables.toArray(split, String.class);

        for(int i = 0; i < parts.length; i++) {
            String key = parts[i];

            if (!(node instanceof ObjectNode)) {
                throw new IllegalArgumentException("Unable to override " + name + "; it's not a valid path.");
            }
            final ObjectNode obj = (ObjectNode) node;

            final String remainingPath = Joiner.on('.').join(Arrays.copyOfRange(parts, i, parts.length));
            if (obj.has(remainingPath) && !remainingPath.equals(key)) {
                if (obj.get(remainingPath).isValueNode()) {
                    obj.put(remainingPath, value);
                    return;
                }
            }

            JsonNode child;
            final boolean moreParts = i < parts.length - 1;

            if (key.matches(".+\\[\\d+\\]$")) {
                final int s = key.indexOf('[');
                final int index = Integer.parseInt(key.substring(s + 1, key.length() - 1));
                key = key.substring(0, s);
                child = obj.get(key);
                if (child == null) {
                    throw new IllegalArgumentException("Unable to override " + name + "; node with index not found.");
                }
                if (!child.isArray()) {
                    throw new IllegalArgumentException("Unable to override " + name + "; node with index is not an array.");
                }
                else if (index >= child.size()) {
                    throw new ArrayIndexOutOfBoundsException("Unable to override " + name + "; index is greater than size of array.");
                }
                if (moreParts) {
                    child = child.get(index);
                    node = child;
                }
                else {
                    ArrayNode array = (ArrayNode)child;
                    array.set(index, TextNode.valueOf(value));
                    return;
                }
            }
            else if (moreParts) {
                child = obj.get(key);
                if (child == null) {
                    child = obj.objectNode();
                    obj.put(key, child);
                }
                if (child.isArray()) {
                    throw new IllegalArgumentException("Unable to override " + name + "; target is an array but no index specified");
                }
                node = child;
            }

            if (!moreParts) {
                if (node.get(key) != null && node.get(key).isArray()) {
                    ArrayNode arrayNode = (ArrayNode) obj.get(key);
                    arrayNode.removeAll();
                    Pattern escapedComma = Pattern.compile("\\\\,");
                    for (String val : Splitter.on(Pattern.compile("(?<!\\\\),")).trimResults().split(value)) {
                        arrayNode.add(escapedComma.matcher(val).replaceAll(","));
                    }
                }
                else {
                    obj.put(key, value);
                }
            }
        }
    }

    public String getLastPath() {
        return lastPath;
    }
}
