package dev.dropwizard.bundler.elastic;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Injector;
import dev.dropwizard.bundler.features.ReflectionsBundle;
import dev.dropwizard.bundler.refmodel.RefModelBundle;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.forName;

/**
 *
 */
public class ElasticRefModelBundle implements Bundle {
    private static final Logger log = LoggerFactory.getLogger(ElasticRefModelBundle.class);

    @Inject Injector injector;
    @Inject ReflectionsBundle reflectionsBundle;
    @Inject IndexResolver indexResolver;
    @Inject RefModelBundle refModelBundle;

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(Environment environment) {
        Set<Class<?>> elasticModels = reflectionsBundle.get().getTypesAnnotatedWith(Elastic.class);
        refModelBundle.addModels(Elastic.class, elasticModels);

        try (ElasticClient.CloseableClient client = injector.getInstance(ElasticClient.class).getClient()) {

            Multimap<String, Class<?>> indexMap = Multimaps.index(elasticModels, new Function<Class<?>, String>() {
                @Nullable
                @Override
                public String apply(@Nullable Class<?> input) {
                    return indexResolver.getIndex(input);
                }
            });
            for (String index : indexMap.keySet()) {
                createIndexIfAbsent(index, client);
                Map<String, Map> mapping = new HashMap<>();
                for (Class<?> type : indexMap.get(index)) {
                    buildMapping(type, mapping);
                }
                putMapping(index, mapping, client);
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not create mapping", e);
        }
    }

    private void buildMapping(Class<?> type, Map<String, Map> mappings) throws NoSuchFieldException {
        for (String testModelKey : refModelBundle.getRefModelKeys(type)) {
            int i1 = testModelKey.indexOf('#');
            String property = testModelKey.substring(i1 + 1);
            String fqn = testModelKey.substring(0, i1);
            String objName = fqn.substring(fqn.lastIndexOf('.') + 1);
            Field field = forName(fqn).getDeclaredField(property);
            Class<?> javaFieldType = field.getType();
            String fieldType = javaFieldType.equals(String.class) ? "string"
                    : javaFieldType.equals(int.class) ? "integer"
                    : javaFieldType.isPrimitive() ? javaFieldType.getSimpleName()
                    : "string"; //fallback

            merge(mappings,
                    map(objName, map(
                            "properties", map(
                                    property, map(
                                            "type", fieldType,
                                            "index", "analyzed",
                                            "analyzer", "standard")),
                            "_all", map(
                                    "enabled", "false"))));
        }
    }

    private void putMapping(String index, Map<String, Map> mappings, ElasticClient.CloseableClient client) {
        for (String type : mappings.keySet()) {
            PutMappingResponse putMappingResponse = client.get().admin().indices().
                    preparePutMapping(index).
                    setType(type).
                    setSource(mappings.get(type)).
                    get();
            if (!putMappingResponse.isAcknowledged()) {
                throw new RuntimeException("Could not create mapping for " + type);
            }
            log.debug("added mapping for {} {}", type, mappings.get(type));
        }
    }

    private void createIndexIfAbsent(String index, ElasticClient.CloseableClient client) {
        if (!client.get().admin().indices().prepareExists(index).get().isExists()) {
            CreateIndexResponse createIndexResponse = client.get().admin().indices().prepareCreate(index).get();
            if (!createIndexResponse.isAcknowledged()) {
                throw new RuntimeException("Could not create index " + index);
            }
        }
    }

    private Map map(Object... objects) {
        Map map = new HashMap();
        for (int i = 0; i < objects.length; i++) {
            map.put(objects[i], objects[++i]);
        }
        return map;
    }

    private void merge(Map mappings, Map map) {
        for (Object k : map.keySet()) {
            if (!mappings.containsKey(k)) {
                mappings.putAll(map);
            } else {
                Object v = map.get(k);
                if (v instanceof Map) {
                    merge((Map) mappings.get(k), (Map) v);
                } else {
                    mappings.put(k, v);
                }
            }
        }
    }
}
