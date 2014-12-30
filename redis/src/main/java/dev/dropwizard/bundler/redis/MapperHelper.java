package dev.dropwizard.bundler.redis;

import com.google.common.collect.Lists;
import redis.clients.jedis.Response;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class MapperHelper extends dev.dropwizard.bundler.refmodel.MapperHelper {

    public <T> T convert(Class<T> objClass, Response<String> response) throws IOException {
        if (response != null) {
            String value = response.get();
            return value != null ? objectMapper.readValue(value, objClass) : null;
        } else {
            return null;
        }
    }

    public <T> List<T> convertAll(Class<T> objClass, Iterable<Response<String>> responses) throws IOException {
        List<T> results = Lists.newArrayList();
        for (Response<String> response : responses) {
            results.add(convert(objClass, response));
        }
        return results;
    }
}
