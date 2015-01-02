package dev.dropwizard.bundler.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class ReflectionsHelper {
    private static final Logger log = LoggerFactory.getLogger(ReflectionsHelper.class);

    public static void expandSupertypes(Reflections reflections, Class<? extends Scanner> scanner, String[][] pkgs) {
        Multimap<String, String> mmap = reflections.getStore().get(scanner.getSimpleName());
        for (String key : Sets.newHashSet(mmap.keySet())) {
            if (!mmap.containsValue(key)) {
                for (String[] packages : pkgs) {
                    if (packages != null) {
                        for (String prefix : packages) {
                            if (key.startsWith(prefix)) {
                                expandSupertypes(mmap, key, ReflectionUtils.forName(key));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void expandSupertypes(Multimap<String, String> mmap, String key, Class<?> type) {
        for (Class<?> supertype : supertypes(type)) {
            if (mmap.put(supertype.getName(), key)) {
                log.debug("expanded subtype {} -> {}", supertype.getName(), key);
            }
            expandSupertypes(mmap, supertype.getName(), supertype);
        }
    }

    private static List<Class<?>> supertypes(Class<?> type) {
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        List<Class<?>> result = new ArrayList<>();
        if (superclass != Object.class && superclass != null) result.add(superclass);
        if (interfaces != null && interfaces.length > 0) result.addAll(Arrays.asList(interfaces));
        return result;
    }
}
