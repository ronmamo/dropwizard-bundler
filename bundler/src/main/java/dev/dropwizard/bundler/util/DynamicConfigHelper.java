package dev.dropwizard.bundler.util;

import com.google.common.collect.Sets;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.not;
import static org.reflections.ReflectionUtils.*;

/**
 *
 */
public class DynamicConfigHelper {

    public static <T> T createDefault(Class<T> aClass) {
        try {
            Constructor<T> constructor = aClass.getConstructor();
            return constructor.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Could not create " + aClass + " using default constructor", e);
        }
    }

    public static <T> void tryToFillFrom(Object source, T target, List<String> dynamicProperties) throws Exception {
        Map sourceGettersMap = getGetters(source.getClass());
        Map targetSettersMap = getSetters(target.getClass());
        Sets.SetView commonProperties = Sets.intersection(sourceGettersMap.keySet(), targetSettersMap.keySet());

        for (Object commonProperty : commonProperties) {
            Object sourceValue = getInvoke(source, sourceGettersMap.get(commonProperty));
            Object targetMember = targetSettersMap.get(commonProperty);
            setInvoke(target, targetMember, sourceValue);
        }

        for (String dynamicProperty : dynamicProperties) {
            Field field = (Field) targetSettersMap.get(dynamicProperty);
            Class<?> type = field.getType();
            Object object = createDefault(type);
            setInvoke(target, field, object);
        }
    }

    public static <T> T getInvoke(Object source, Object sourceMember, Object... args) {
        try {
            if (sourceMember instanceof Method) {
                return (T) ((Method) sourceMember).invoke(source, args);
            } else if (sourceMember instanceof Field) {
                Field field = (Field) sourceMember;
                field.setAccessible(true);
                return (T) field.get(source);
            } else if (sourceMember instanceof String) {
                Object o = getGetters(source.getClass()).get(sourceMember);
                return getInvoke(source, o, args);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void setInvoke(T target, Object targetMember, Object sourceValue) {
        try {
            if (targetMember instanceof Method) {
                ((Method) targetMember).invoke(target, sourceValue);
            } else if (targetMember instanceof Field) {
                Field field = (Field) targetMember;
                field.setAccessible(true);
                field.set(target, sourceValue);
            } else if (targetMember instanceof String) {
                Member o = getSetters(target.getClass()).get(targetMember);
                setInvoke(target, o, sourceValue);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Map<String, ? extends Member> getGetters(Class<T> aClass) {
        Set getters = getAllMethods(aClass,
                withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0), not(withReturnType(void.class)));
        return getPropertyMap(aClass, getters);
    }

    public static <T> Map<String, ? extends Member> getSetters(Class aClass) {
        Set<Method> setters = getAllMethods(aClass,
                withModifier(Modifier.PUBLIC), withPrefix("set"), withParametersCount(1));
        return getPropertyMap(aClass, setters);
    }

    private static <T> Map<String, ? extends Member> getPropertyMap(Class<T> aClass, Set<Method> members) {
        Map<String, Member> propertyMap = new HashMap<>();
        for (Object member : members) {
            Method input = (Method) member;
            propertyMap.put(input.getName().substring(3, 4).toLowerCase() + input.getName().substring(5), input);
        }
        for (Field field : getAllFields(aClass)) {
            propertyMap.put(field.getName(), field);
        }
        return propertyMap;
    }
}
