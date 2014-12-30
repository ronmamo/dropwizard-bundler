package dev.dropwizard.bundler.features;

import com.google.inject.ImplementedBy;

import java.util.Comparator;

/**
 */
@ImplementedBy(BundlesOrder.ByName.class)
public interface BundlesOrder extends Comparator<Object> {

    public static class ByName implements BundlesOrder {
        @Override
        public int compare(Object o1, Object o2) {
            return o1.getClass().getName().compareTo(o2.getClass().getName());
        }
    }
}
