package dev.dropwizard.bundler.elastic;

import com.google.inject.ImplementedBy;

/**
 */
@ImplementedBy(IndexResolver.Default.class)
public interface IndexResolver {
    String getIndex(Class<?> objClass);

    public static class Default implements IndexResolver {
        @Override
        public String getIndex(Class<?> objClass) {
            return "default";
        }
    }
}
