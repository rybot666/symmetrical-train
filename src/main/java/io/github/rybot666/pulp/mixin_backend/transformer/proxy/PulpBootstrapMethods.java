package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import org.objectweb.asm.tree.ClassNode;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bootstrap methods used by generated classes (invokedynamic)
 */
public class PulpBootstrapMethods {
    private static final MethodHandle SELF_GET_PROXY_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            SELF_GET_PROXY_HANDLE = lookup.findStatic(
                    PulpBootstrapMethods.class,
                    "getProxyImpl",
                    MethodType.methodType(Object.class, Map.class, Class.class, Object.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Failed to get a method handle for BSMs", e);
        }
    }

    // Method used for indy invocation after map reflectively located from proxy class type
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter") // instanceMap is a reflected static
    public static Object getProxyImpl(Map<Object, Object> instanceMap, Class<? extends ProxyMarker> proxyClazz, Object instance) {
        synchronized (instanceMap) {
            Object proxyInstance = instanceMap.get(instance);

            if (proxyInstance != null) {
                return proxyInstance;
            }

            try {
                Constructor<?> constructor = proxyClazz.getDeclaredConstructor(instance.getClass());
                proxyInstance = constructor.newInstance(instance);

                instanceMap.put(instance, proxyInstance);
                return proxyInstance;
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                throw new AssertionError("Proxy class has invalid or missing constructor", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Proxy class constructor threw an error", e);
            }
        }
    }

    // Returns a handle that takes a class instance and returns a matching proxy instance
    @SuppressWarnings("unchecked")
    public static CallSite getProxy(MethodHandles.Lookup caller, String name, MethodType type, Class<?> targetClazz) {
        Class<? extends ProxyMarker> proxyClazz = ProxyFactory.PROXY_CLASS_MAP.get(targetClazz.getName().replace('.', '/'));

        if (proxyClazz == null) {
            throw new IllegalArgumentException("The provided target class does not have a proxy class assigned");
        }

        // Proxy class has an INSTANCES static field with a map of target instance to proxy instance
        try {
            Field instancesField = proxyClazz.getDeclaredField("INSTANCES");
            assert Modifier.isStatic(instancesField.getModifiers());

            WeakHashMap<Object, Object> instanceMap = (WeakHashMap<Object, Object>) instancesField.get(null);
            MethodHandle implHandle = MethodHandles.insertArguments(SELF_GET_PROXY_HANDLE, 0, instanceMap, proxyClazz);

            return new ConstantCallSite(implHandle.asType(type));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Invalid proxy class - threw error during get indy", e);
        }
    }
}
