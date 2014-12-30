package dev.dropwizard.bundler.redis;

import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import dev.dropwizard.bundler.refmodel.RefModelBundle;
import dev.dropwizard.bundler.swagger.SwaggerBundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.reflections.Reflections;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.net.URL;
import java.util.Set;

/**
*
*/
public class RedisBundle implements ConfiguredBundle<RedisBundle.Configuration>, Module {

    @Inject Reflections reflections;
    @Inject SwaggerBundle swaggerBundle;
    @Inject RefModelBundle refModelBundle;

    private JedisPool jedisPool;

    public static class Configuration extends io.dropwizard.Configuration {
        public Redis redis;

        public static class Redis extends GenericObjectPool.Config {
            public String host;
            public int port;
            public String url;
        }
    }

    @Provides
    public JedisPool jedisPool() {
        return jedisPool;
    }

    @Override
    public void configure(Binder binder) {
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(RedisBundle.Configuration configuration, Environment environment) throws Exception {
        Set<Class<?>> redisModels = reflections.getTypesAnnotatedWith(Redis.class);
        swaggerBundle.configure(configuration, "localhost", "redis", redisModels, refModelBundle.getPropertyMap());

        Configuration.Redis conf = configuration.redis;
        String host = conf.host;
        int port = conf.port;

        if (!Strings.isNullOrEmpty(conf.url)) {
            URL url = new URL(conf.url);
            host = url.getHost();
            port = url.getPort();
        }

        jedisPool = new JedisPool(conf, host, port);
    }
}
