package io.github.rybot666.pulp.util.reflect;

import java.lang.reflect.Field;

public class FieldUtils {
    private FieldUtils() {
        throw new RuntimeException("Cannot instantiate utility class");
    }

    public static Object get(Object obj, Class<?> clazz, String name, boolean force) {
        try {
            Field field = clazz.getDeclaredField(name);

            if (force) {
                field.setAccessible(true);
            }

            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                if (!force) {
                    return null;
                }

                throw e;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Reflection error", e);
        }
    }

    public static Object get(Object obj, Class<?> clazz, String name) {
        return get(obj, clazz, name, true);
    }

    public static Object get(Object obj, String name, boolean force) {
        return get(obj, obj.getClass(), name, force);
    }

    public static Object get(Object obj, String name) {
        return get(obj, name, true);
    }

    public static Object getStatic(Class<?> clazz, String name, boolean force) {
        return get(null, clazz, name, force);
    }

    public static Object getStatic(Class<?> clazz, String name) {
        return get(null, clazz, name, true);
    }
}
