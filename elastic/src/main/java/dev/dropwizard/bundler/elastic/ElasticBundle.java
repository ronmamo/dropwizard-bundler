package dev.dropwizard.bundler.elastic;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import dev.dropwizard.bundler.features.ReflectionsBundle;
import dev.dropwizard.bundler.refmodel.RefModelBundle;
import dev.dropwizard.bundler.swagger.SwaggerBundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Set;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 *
 */
public class ElasticBundle implements ConfiguredBundle<ElasticBundle.Configuration>, Module {

    @Inject ReflectionsBundle reflectionsBundle;
    @Inject SwaggerBundle swaggerBundle;
    @Inject RefModelBundle refModelBundle;
    private Node node;

    public static class Configuration extends io.dropwizard.Configuration {
        public Elastic elastic;

        public static class Elastic extends HashMap {
        }
    }

    @Provides
    public Node node() {
        return node;
    }

    @Override
    public void configure(Binder binder) {
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(final ElasticBundle.Configuration configuration, Environment environment) throws Exception {
        Set<Class<?>> elasticModels = reflectionsBundle.get().getTypesAnnotatedWith(Elastic.class);
        swaggerBundle.configure(configuration, "localhost", "elastic", elasticModels, refModelBundle.getPropertyMap());

        ImmutableSettings.Builder settings = ImmutableSettings.builder();
        settings.put(configuration.elastic);
        node = nodeBuilder().settings(settings).node();
    }
}
