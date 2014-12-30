package dev.dropwizard.bundler.refmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class MapperHelper {

    @Inject public ObjectMapper objectMapper;

    public <T> T read(Class<T> objClass, String value) {
        try {
            return objectMapper.readValue(value, objClass);
        } catch (IOException e) {
            throw new RuntimeException("Could not read object " + objClass + " [" + value + "]", e);
        }
    }

    public <T> String write(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write object " + object, e);
        }
    }

    public <T> String getPropertyValue(T object, Object property) {
        String id;
        try {
            if (object instanceof Map) {
                id = (String) ((Map) object).get(property);
            } else {
                id = object.getClass().getField("" + property).get(object).toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not get property " + property + " for " + object, e);
        }
        if (id == null) {
            throw new RuntimeException("Could not get property " + property + " for " + object);
        }
        return id;
    }
}
