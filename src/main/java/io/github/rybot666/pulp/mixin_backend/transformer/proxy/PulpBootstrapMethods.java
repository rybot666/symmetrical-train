package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Bootstrap methods used by generated classes (invokedynamic)
 */
public class PulpBootstrapMethods {
    private static final MethodHandle MAP_GET_HANDLE;

    static {
        try {
            MAP_GET_HANDLE = MethodHandles.lookup()
                    .findVirtual(Map.class, "get", MethodType.methodType(Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Somehow failed to get Map#get method as a methodhandle!", e);
        }
    }

    // Gets the proxy class for the provided instance of a class
    public static CallSite getProxy(MethodHandles.Lookup caller, String name, MethodType type, Class<?> targetClazz) {
        Class<? extends ProxyMarker> proxyClazz = ProxyFactory.PROXY_CLASS_MAP.get(targetClazz.getName().replace('.', '/'));

        if (proxyClazz == null) {
            throw new IllegalArgumentException("The provided target class does not have a proxy class assigned");
        }

        return new ConstantCallSite(MAP_GET_HANDLE);
    }
}
