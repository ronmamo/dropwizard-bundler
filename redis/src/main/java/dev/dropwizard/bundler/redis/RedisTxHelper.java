package dev.dropwizard.bundler.redis;

import com.google.common.collect.Lists;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class RedisTxHelper {

    @Inject JedisPool jedisPool;

    public interface TxOperation<T> {
        Response<T> execute(Transaction tx) throws Exception;
    }

    public <T> Response<T> executeInTx(TxOperation<T> operation) throws Exception {
        return executeAllInTx(operation).get(0);
    }

    public <T> List<Response<T>> executeAllInTx(TxOperation<T>... operations) throws Exception {
        return executeAllInTx(Arrays.asList(operations));
    }

    public <T> List<Response<T>> executeAllInTx(Iterable<? extends TxOperation<T>> operations) throws Exception {
        Jedis redis = null;
        Transaction transaction = null;
        try {
            redis = jedisPool.getResource();
            transaction = redis.multi();
            List<Response<T>> results = Lists.newArrayList();
            for (TxOperation<T> operation : operations) {
                results.add(operation.execute(transaction));
            }
            List<Object> execResult = transaction.exec();
            return results;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.discard();
            }
            throw new RuntimeException("Could not executeInTransaction", e);
        } finally {
            if (redis != null) {
                jedisPool.returnResource(redis);
            }
        }
    }
}
