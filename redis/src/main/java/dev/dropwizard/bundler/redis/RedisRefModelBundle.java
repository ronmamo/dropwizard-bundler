package dev.dropwizard.bundler.redis;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import dev.dropwizard.bundler.refmodel.RefModelBundle;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Transaction;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

/**
*
*/
public class RedisRefModelBundle implements ConfiguredBundle<Configuration>, Module {

    public static final String KEY_PREFIX = "keyPrefix";
    public static final String ID_PROPERTY = "id";

    @Inject Injector injector;
    @Inject RefModelBundle refModelBundle;
    @Inject Reflections reflections;
    private PutByPropertyRefModelInterceptor interceptor;

    @Provides
    @Named(KEY_PREFIX)
    public String keyPrefix() {
        return "v1";
    }

    @Override
    public void configure(Binder binder) {
        try {
            interceptor = new PutByPropertyRefModelInterceptor();

            binder.bindInterceptor(Matchers.only(RedisClient.class),
                    Matchers.only(RedisClient.class.getDeclaredMethod("put", Transaction.class, Object.class)),
                    interceptor);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not bind interceptors for " + RedisClient.class, e);
        }
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    public void run(Configuration configuration, Environment environment) throws Exception {
        Set<Class<?>> redisModels = reflections.getTypesAnnotatedWith(Redis.class);
        refModelBundle.addModels(Redis.class, redisModels);
        injector.injectMembers(interceptor);
    }

    private static class PutByPropertyRefModelInterceptor implements MethodInterceptor {
        private static final Logger log = LoggerFactory.getLogger(PutByPropertyRefModelInterceptor.class);

        @Inject RedisClient redisClient;
        @Inject RefModelBundle refModelBundle;

        @Override
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            try {
                Object proceed = methodInvocation.proceed();
                Transaction transaction = (Transaction) methodInvocation.getArguments()[0];
                Object object = methodInvocation.getArguments()[1];

                put(transaction, object);
                return proceed;
            } catch (Exception e) {
                log.error("Could not putByProperty", e);
                throw new RuntimeException("Could not putByProperty", e);
            }
        }

        private void put(Transaction transaction, Object object) throws Exception {
            for (String modelName : refModelBundle.getRefModelKeys(object.getClass())) {
                String objName = object.getClass().getName();
                String property = modelName.substring(objName.length() + 1);
                if (!property.equals(ID_PROPERTY)) {
                    redisClient.putByProperty(transaction, object, property);
                }
            }
        }
    }
}
