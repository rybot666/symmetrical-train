package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;

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
                    MethodType.methodType(Object.class, Map.class, Constructor.class, Class.class, Object.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Failed to get a method handle for BSMs", e);
        }
    }

    // Method used for indy invocation after map reflectively located from proxy class type
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter") // instanceMap is a reflected static
    public static Object getProxyImpl(Map<Object, Object> instanceMap, Constructor<?> ctor, Class<?> desiredInstanceClazz, Object instance) {
        synchronized (instanceMap) {
            Object proxyInstance = instanceMap.get(instance);

            if (proxyInstance != null) {
                return proxyInstance;
            }

            if (!desiredInstanceClazz.isAssignableFrom(instance.getClass())) {
                throw new AssertionError(String.format("Proxy constructor desired class (%s) is not assignable from actual class (%s)", desiredInstanceClazz, instance.getClass()));
            }

            try {
                Object desiredInstance = desiredInstanceClazz.cast(instance);
                proxyInstance = ctor.newInstance(desiredInstance);

                instanceMap.put(instance, proxyInstance);
                return proxyInstance;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new AssertionError(String.format("Proxy class has invalid or missing constructor (%s)", ctor), e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Proxy class constructor threw an error", e);
            }
        }
    }

    // Returns a handle that takes a class instance and returns a matching proxy instance
    public static CallSite getProxy(@SuppressWarnings("unused") MethodHandles.Lookup caller, @SuppressWarnings("unused") String name, MethodType type) {
        // Pull the type of the proxy class from the requested type signature
        Class<?> maybeProxyClazz = type.returnType();

        if (!ProxyMarker.class.isAssignableFrom(maybeProxyClazz)) {
            throw new IllegalArgumentException("The proxy class does not implement required marker interface");
        }

        Class<? extends ProxyMarker> proxyClazz = maybeProxyClazz.asSubclass(ProxyMarker.class);

        // Pull the type of the instance class from the requested type signature
        Class<?> instanceClazz = type.parameterType(0);

        // Locate desired constructor
        Constructor<?>[] ctors = proxyClazz.getDeclaredConstructors();

        if (ctors.length != 1) {
            throw new AssertionError(String.format("Proxy class has wrong number of constructors (has %d needs 1)", ctors.length));
        }

        Constructor<?> ctor = ctors[0];
        Class<?>[] ctorArgs = ctor.getParameterTypes();

        if (ctorArgs.length != 1) {
            throw new AssertionError(String.format("Proxy class constructor has wrong number of arguments (has %d needs 1)", ctorArgs.length));
        }

        Class<?> desiredInstanceClazz = ctorArgs[0];

        if (!desiredInstanceClazz.isAssignableFrom(instanceClazz)) {
            throw new AssertionError(String.format("Proxy class constructor parameter (%s) is not assignable from provided instance class (%s)", desiredInstanceClazz, instanceClazz));
        }

        // Proxy classes have an INSTANCES static field with a map of target instance to proxy instance
        try {
            Field instancesField = proxyClazz.getDeclaredField("INSTANCES");
            assert Modifier.isStatic(instancesField.getModifiers());

            @SuppressWarnings("unchecked")
            Map<Object, Object> instanceMap = (Map<Object, Object>) instancesField.get(null);
            MethodHandle implHandle = MethodHandles.insertArguments(SELF_GET_PROXY_HANDLE, 0, instanceMap, ctor, desiredInstanceClazz);

            return new ConstantCallSite(implHandle.asType(type));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Invalid proxy class - threw error during get indy", e);
        }
    }
}
