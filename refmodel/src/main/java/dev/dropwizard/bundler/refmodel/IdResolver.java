package dev.dropwizard.bundler.refmodel;

import com.google.inject.ImplementedBy;
import dev.dropwizard.bundler.features.DynamicConfigHelper;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Set;

import static org.reflections.ReflectionUtils.getFields;
import static org.reflections.ReflectionUtils.withAnnotation;

/**
 *
 */
@ImplementedBy(IdResolver.Composite.class)
public interface IdResolver {
    @Nullable Object getId(Object object);

    public static class ByName implements IdResolver {
        @Inject MapperHelper mapper;

        @Override
        public Object getId(Object object) {
            return mapper.getPropertyValue(object, "id");
        }
    }

    public static class ByAnnotation implements IdResolver {
        @Override
        public Object getId(Object object) {
            Field idField = getIdField(object);
            if (idField != null) {
                return DynamicConfigHelper.getInvoke(object, idField);
            } else {
                return null;
            }
        }

        //todo cache it
        private Field getIdField(Object object) {
            Set<Field> fields = getFields(object.getClass(), withAnnotation(Id.class));
            if (!fields.isEmpty()) {
                Field idField = fields.iterator().next();
                idField.setAccessible(true);
                return idField;
            } else {
                return null;
            }
        }
    }

    public static class Composite implements IdResolver {
        @Inject ByAnnotation byAnnotation;
        @Inject ByName byName;

        @Nullable
        @Override
        public Object getId(Object object) {
            Object id = byAnnotation.getId(object);
            if (id == null) {
                id = byName.getId(object);
            }
            if (id == null) {
                throw new RuntimeException("Could not get object id [" + object + "]");
            }
            return id;
        }
    }
}
