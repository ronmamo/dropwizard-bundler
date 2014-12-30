package dev.dropwizard.bundler.swagger;

import com.wordnik.swagger.jaxrs.config.JaxrsScanner;
import scala.collection.immutable.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import java.util.Collection;

/**
*
*/
public class ModelClassScanner implements JaxrsScanner {
    private Collection<Class<?>> types;

    public ModelClassScanner(Collection<Class<?>> redisModels) {
        types = redisModels;
    }

    @Override
    public List<Class<?>> classesFromContext(Application app, ServletConfig sc) {
        return classes();
    }

    @Override
    public List<Class<?>> classes() {
        return List.<Class<?>>fromArray(types.toArray(new Class[types.size()]));
    }
}
