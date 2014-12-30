package dev.dropwizard.bundler.redis;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import dev.dropwizard.bundler.refmodel.IdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static dev.dropwizard.bundler.redis.RedisTxHelper.TxOperation;

/**
*
*/
public class RedisClient {
    private static final Logger log = LoggerFactory.getLogger(RedisClient.class);

    @Inject KeyResolver keyResolver;
    @Inject IdResolver idResolver;
    @Inject MapperHelper mapper;
    @Inject RedisTxHelper redisTxHelper;

    public <T> T get(Class<T> objClass, String id) {
        try {
            final String key = keyResolver.getKey(objClass.getSimpleName(), null, id);
            return mapper.convert(objClass, redisTxHelper.executeInTx(new TxOperation<String>() {
                @Override
                public Response<String> execute(Transaction tx) throws Exception {
                    return get(tx, key);
                }
            }));
        } catch (Exception e) {
            throw new RuntimeException("Could not get object [" + objClass + "] [" + id + "]", e);
        }
    }

    public <T> boolean put(final T object) {
        try {
            Response<Long> response = redisTxHelper.executeInTx(new TxOperation<Long>() {
                @Override
                public Response<Long> execute(Transaction tx) throws Exception {
                    return put(tx, object);
                }
            });
            Long res = response.get();
            return res != 0;
        } catch (Exception e) {
            throw new RuntimeException("Could not put object [" + object + "]", e);
        }
    }

    public <T> List<T> getByProperty(final Class<T> objClass, final Object property, final Object value) throws Exception {
        final String key = keyResolver.getKey(objClass.getSimpleName(), "" + property, "" + value);
        Response<Set<String>> response = redisTxHelper.executeInTx(new RedisTxHelper.TxOperation<Set<String>>() {
            @Override
            public Response<Set<String>> execute(Transaction tx) throws Exception {
                return tx.smembers(key);
            }
        });
        Set<String> strings = response.get();
        return getAll(objClass, strings);
    }

    //todo get tx from ThreadLocal
    public Response<Long> putByProperty(Transaction transaction, final Object object, final String property) throws Exception {
        final String key = keyResolver.getKey(object, property);
        final String value = idResolver.getId(object).toString();
        Response<Long> response = transaction.sadd(key, value);
        log.debug("putByProperty {} [{}]", key, value);
        return response;
    }

    Response<String> get(Transaction tx, String key) {
        return tx.get(key);
    }

    //intercepted
    <T> Response<Long> put(Transaction tx, T object) {
        final String key = keyResolver.getKey(object.getClass().getSimpleName(), null, "" + idResolver.getId(object));
        final String value = mapper.write(object);
        Response<Long> response = tx.setnx(key, value);
        log.debug("put {} [{}]", key, value);
        return response;
    }

    <T> List<T> getAll(final Class<T> objClass, Iterable<String> ids) {
        try {
            return mapper.convertAll(objClass,
                    redisTxHelper.executeAllInTx(
                            FluentIterable.from(ids).transform(new Function<String, TxOperation<String>>() {
                                @Nullable
                                @Override
                                public RedisTxHelper.TxOperation<String> apply(final String id) {
                                    return new RedisTxHelper.TxOperation<String>() {
                                        @Override
                                        public Response<String> execute(Transaction tx) throws Exception {
                                            return tx.get(keyResolver.getKey(objClass.getSimpleName(), null, id));
                                        }
                                    };
                                }
                            })
                    ));
        } catch (Exception e) {
            throw new RuntimeException("Could not get objects [" + objClass + "] from ids " + ids, e);
        }
    }

    //for test purposes
    public void flushDB() throws Exception {
        redisTxHelper.executeInTx(new TxOperation<String>() {
            @Override
            public Response<String> execute(Transaction tx) throws Exception {
                return tx.flushDB();
            }
        });
    }
}
