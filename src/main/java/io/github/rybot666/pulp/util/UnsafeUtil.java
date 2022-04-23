package io.github.rybot666.pulp.util;

import io.github.rybot666.pulp.util.reflect.FieldUtils;
import org.bukkit.Bukkit;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public class UnsafeUtil {
    private static final sun.misc.Unsafe UNSAFE = (Unsafe) FieldUtils.getStatic(Unsafe.class, "theUnsafe");

    private UnsafeUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static Object getFieldWithUnsafe(Object instance, Class<?> ownerClass, String fieldName) throws NoSuchFieldException {
        return UNSAFE.getObject(instance, UNSAFE.objectFieldOffset(ownerClass.getDeclaredField(fieldName)));
    }

    public static boolean getBooleanWithUnsafe(Object instance, Class<?> ownerClass, String fieldName) throws NoSuchFieldException {
        return UNSAFE.getBoolean(instance, UNSAFE.objectFieldOffset(ownerClass.getDeclaredField(fieldName)));
    }

    /**
     * Emulates URLClassLoader#addUrl with Unsafe
     *
     * @param loader the target classloader
     * @param url the URL to add
     */
    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    public static void addToURLClassPath(URLClassLoader loader, URL url) {
        try {
            Object ucp = getFieldWithUnsafe(loader, loader.getClass(), "ucp");

            synchronized (ucp) {
                if (getBooleanWithUnsafe(ucp, ucp.getClass(), "closed")) {
                    throw new AssertionError("UCP is closed");
                }

                Collection<URL> urls = (Collection<URL>) getFieldWithUnsafe(ucp, ucp.getClass(), "unopenedUrls");
                synchronized (urls) {
                    Collection<URL> path = (Collection<URL>) getFieldWithUnsafe(ucp, ucp.getClass(), "path");
                    if (!path.contains(url)) {
                        urls.add(url);
                        path.add(url);
                    }
                }
            }
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing expected field of URLClassLoader", e);
        }
    }
}
