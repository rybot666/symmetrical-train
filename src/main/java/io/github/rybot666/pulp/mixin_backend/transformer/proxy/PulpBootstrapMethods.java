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

// TODO we should probably get a lookup from the target class on proxy creation but this is slow
public class PulpBootstrapMethods {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
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
        Object proxyInstance;

        synchronized (instanceMap) {
            proxyInstance = instanceMap.get(instance);
        }

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

    private static MethodHandle getProxyMethodHandle(Class<?> proxyClazz, Class<?> instanceClazz) {
        // Locate desired constructor
        Constructor<?>[] ctors = proxyClazz.getDeclaredConstructors();
        assert ctors.length == 1;

        Constructor<?> ctor = ctors[0];

        Class<?>[] ctorArgs = ctor.getParameterTypes();
        assert ctorArgs.length == 1;

        Class<?> desiredInstanceClazz = ctorArgs[0];

        assert desiredInstanceClazz.isAssignableFrom(instanceClazz);

        // Proxy classes have an INSTANCES static field with a map of target instance to proxy instance
        try {
            Field instancesField = proxyClazz.getDeclaredField("INSTANCES");
            assert Modifier.isStatic(instancesField.getModifiers());

            @SuppressWarnings("unchecked")
            Map<Object, Object> instanceMap = (Map<Object, Object>) instancesField.get(null);

            return MethodHandles.insertArguments(SELF_GET_PROXY_HANDLE, 0, instanceMap, ctor, desiredInstanceClazz);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Invalid proxy class - threw error during get indy", e);
        }
    }


    // Returns a handle that takes a class instance and returns a matching proxy instance
    // Called as `getProxy(Linstance/here;)Ltarget/proxy/type;`
    public static CallSite getProxy(@SuppressWarnings("unused") MethodHandles.Lookup caller, @SuppressWarnings("unused") String name, MethodType type) {
        // Pull the type of the proxy class from the requested type signature
        Class<?> maybeProxyClazz = type.returnType();

        if (!ProxyMarker.class.isAssignableFrom(maybeProxyClazz)) {
            throw new IllegalArgumentException("The proxy class does not implement required marker interface");
        }

        Class<? extends ProxyMarker> proxyClazz = maybeProxyClazz.asSubclass(ProxyMarker.class);

        // Pull the type of the instance class from the requested type signature
        Class<?> instanceClazz = type.parameterType(0);

        MethodHandle implHandle = getProxyMethodHandle(proxyClazz, instanceClazz);

        return new ConstantCallSite(implHandle.asType(type));
    }

    // Collection of methods to get and set private fields of objects

    // Called as `getPrivateField(Ltarget/class;)Lfield/type;`
    // Method name is used as field name
    public static CallSite getPrivateField(@SuppressWarnings("unused") MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchFieldException, IllegalAccessException {
        assert type.parameterCount() == 1;

        Class<?> targetClass = type.parameterType(0);
        Class<?> fieldType = type.returnType();

        MethodHandle handle = MethodHandles.privateLookupIn(targetClass, LOOKUP)
                .findGetter(targetClass, name, fieldType);
        return new ConstantCallSite(handle);
    }

    // Called as `setPrivateField(Ltarget/class;Lfield/type;)V`
    // Method name is used as field name
    public static CallSite setPrivateField(@SuppressWarnings("unused") MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchFieldException, IllegalAccessException {
        assert type.parameterCount() == 2;
        assert type.returnType() == void.class;

        Class<?> targetClass = type.parameterType(0);
        Class<?> fieldType = type.parameterType(1);

        MethodHandle handle = MethodHandles.privateLookupIn(targetClass, LOOKUP)
                .findSetter(targetClass, name, fieldType);
        return new ConstantCallSite(handle);
    }

    // Called as `getStaticField()Lfield/type;`
    // Method name is used as field name
    public static CallSite getStaticField(@SuppressWarnings("unused") MethodHandles.Lookup lookup, String name, MethodType type, Class<?> targetClass) throws NoSuchFieldException, IllegalAccessException {
        assert type.parameterCount() == 0;

        Class<?> fieldType = type.returnType();

        MethodHandle handle = MethodHandles.privateLookupIn(targetClass, LOOKUP)
                .findStaticGetter(targetClass, name, fieldType);
        return new ConstantCallSite(handle);
    }

    // Called as `setStaticField(Lfield/type;)V`
    // Method name is used as field name
    public static CallSite setStaticField(@SuppressWarnings("unused") MethodHandles.Lookup lookup, String name, MethodType type, Class<?> targetClass) throws NoSuchFieldException, IllegalAccessException {
        assert type.parameterCount() == 1;
        assert type.returnType() == void.class;

        Class<?> fieldType = type.parameterType(0);

        MethodHandle handle = MethodHandles.privateLookupIn(targetClass, LOOKUP)
                .findStaticSetter(targetClass, name, fieldType);
        return new ConstantCallSite(handle);
    }
}
