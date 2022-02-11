package io.github.rybot666.pulp.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public class UnsafeUtil {
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to get unsafe", e);
        }
    }

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
            Object ucp = getFieldWithUnsafe(Bukkit.class.getClassLoader(), URLClassLoader.class, "ucp");

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
