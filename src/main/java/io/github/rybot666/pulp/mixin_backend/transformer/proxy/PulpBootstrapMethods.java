package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import io.github.rybot666.pulp.proxies.GlobalProxyState;

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
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodHandle SELF_GET_PROXY_HANDLE;
    private static final MethodHandle SELF_GET_DEFINALIZED_FIELD_HANDLE;
    private static final MethodHandle SELF_SET_DEFINALIZED_FIELD_HANDLE;
    private static final MethodHandle SELF_INVOKE_PROXIFIED_METHOD_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            SELF_GET_PROXY_HANDLE = lookup.unreflect(PulpBootstrapMethods.class.getDeclaredMethod("getProxyImpl", Map.class, Constructor.class, Class.class, Object.class));
            SELF_GET_DEFINALIZED_FIELD_HANDLE = lookup.unreflect(PulpBootstrapMethods.class.getDeclaredMethod("getDefinalizedFieldImpl", MethodHandle.class, VarHandle.class, VarHandle.class, VarHandle.class, Object.class));
            SELF_SET_DEFINALIZED_FIELD_HANDLE = lookup.unreflect(PulpBootstrapMethods.class.getDeclaredMethod("setDefinalizedFieldImpl", MethodHandle.class, VarHandle.class, VarHandle.class, Object.class, Object.class));
            SELF_INVOKE_PROXIFIED_METHOD_HANDLE = lookup.unreflect(PulpBootstrapMethods.class.getDeclaredMethod("invokeProxifiedMethodImpl", MethodHandle.class, MethodHandle.class, Object.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Failed to get a method handle for BSMs (developer error)", e);
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

    // Returns a handle that takes a class instance and returns a matching proxy instance
    //
    // The return type is an object because the proxy instance is obtained lazily on the first call to this so may not
    // actually be loaded yet
    //
    // Called as `getProxy(Linstance/here;)Ljava/lang/Object;`
    public static CallSite getProxy(MethodHandles.Lookup caller, MethodType type, @SuppressWarnings("unused") String name) {
        assert type.returnType() == Object.class;
        assert type.parameterCount() == 1;

        // The proxy may or may not be defined yet
        // If a proxy class exists pull it from the map
        Class<?> instanceClazz = type.parameterType(0);
        Class<?> proxyClazz = getOrCreateProxyClass(caller, instanceClazz);

        MethodHandle handle = createHandleForGetProxy(proxyClazz, instanceClazz);

        return new ConstantCallSite(handle.asType(type));
    }

    private static Class<? extends ProxyMarker> getOrCreateProxyClass(MethodHandles.Lookup caller, Class<?> instanceClass) {
        String instanceInternal = instanceClass.getName().replace('.', '/');

        Class<? extends ProxyMarker> proxyClazz;
        synchronized (GlobalProxyState.PROXY_CLASS_MAP) {
            proxyClazz = GlobalProxyState.PROXY_CLASS_MAP.get(instanceInternal);
        }

        if (proxyClazz == null) {
            byte[] proxyBytes;

            synchronized (GlobalProxyState.WAITING_PROXY_CLASSES) {
                proxyBytes = GlobalProxyState.WAITING_PROXY_CLASSES.get(instanceInternal);
            }

            if (proxyBytes == null) {
                throw new IllegalStateException(String.format("Attempted to get proxy class for a class with no assigned proxy (class name %s).", instanceInternal));
            }

            try {
                // This is safe because all proxy classes have this marker interface already
                //noinspection unchecked
                proxyClazz = (Class<? extends ProxyMarker>) caller.defineClass(proxyBytes);

                synchronized (GlobalProxyState.PROXY_CLASS_MAP) {
                    GlobalProxyState.PROXY_CLASS_MAP.put(instanceInternal, proxyClazz);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError("Failed to define proxy class using indy lookup", e);
            }
        }

        return proxyClazz;
    }

    private static MethodHandle createHandleForGetProxy(Class<?> proxyClazz, Class<?> instanceClazz) {
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

    // Collection of methods to get and set private fields of objects

    // Called as `getPrivateField(Ltarget/class;)Lfield/type;`
    // Method name is used as field name
    public static CallSite getPrivateField(@SuppressWarnings("unused") MethodHandles.Lookup lookup, String name, MethodType type) throws NoSuchFieldException, IllegalAccessException {
        assert type.parameterCount() == 1;
        assert type.returnType() != void.class;

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

    // Indy that possibly copies the original field into the cloned proxy one
    //
    // This is required because classes can already have been initialised when they are transformed, so this cannot be
    // done in the constructor
    //
    // Called as `fieldName(thisInstance)`, the field info and target classes are set by the indy BSM types
    public static CallSite getDefinalizedField(MethodHandles.Lookup caller, String fieldName, MethodType type) throws IllegalAccessException, NoSuchFieldException {
        assert type.parameterCount() == 1;
        assert type.returnType() != void.class;

        // Get proxy class
        Class<?> instanceClass = type.parameterType(0);
        Class<? extends ProxyMarker> proxyClass = getOrCreateProxyClass(caller, instanceClass);

        // Grab the field type from the parameters list
        Class<?> fieldType = type.returnType();

        // Find appropriate handles
        MethodHandles.Lookup proxyLookup = MethodHandles.privateLookupIn(proxyClass, caller);
        VarHandle initHandle = proxyLookup.findVarHandle(proxyClass, fieldName.concat("$initialized"), boolean.class);
        VarHandle fieldHandle = proxyLookup.findVarHandle(proxyClass, fieldName, fieldType);

        VarHandle originalHandle = caller.findVarHandle(instanceClass, fieldName, fieldType);

        // Generate getProxy handle
        MethodHandle proxyCreationHandle = createHandleForGetProxy(proxyClass, instanceClass);

        // Generate method handle with arguments inserted (so they're cached)
        MethodHandle handle = MethodHandles.insertArguments(SELF_GET_DEFINALIZED_FIELD_HANDLE, 0, proxyCreationHandle, proxyCreationHandle, initHandle, fieldHandle, originalHandle);
        return new ConstantCallSite(handle.asType(type));
    }

    // TODO (opt) the check can be replaced with the proxy field get method handle after initialization using a MutableCallSite
    public static Object getDefinalizedFieldImpl(MethodHandle proxyCreationHandle, VarHandle initHandle, VarHandle fieldHandle, VarHandle originalHandle, Object instance) throws Throwable {
        // Get or create proxy
        Object proxy = proxyCreationHandle.invoke(initHandle);

        // Check if the field was already initialized
        boolean isInitialized = (boolean) initHandle.get(proxy);

        if (!isInitialized) {
            // If it wasn't copy the old value and then set the init flag to true
            Object old = originalHandle.get(instance);
            fieldHandle.set(proxy, old);

            initHandle.set(proxy, true);

            // And return the value we just stored (prevents one access)
            return old;
        }

        // Return the (possibly just initialized) contents of the field
        return fieldHandle.get(proxy);
    }

    public static CallSite setDefinalizedField(MethodHandles.Lookup caller, String fieldName, MethodType type) throws IllegalAccessException, NoSuchFieldException {
        assert type.parameterCount() == 2;
        assert type.returnType() == void.class;

        // Get proxy class
        Class<?> instanceClass = type.parameterType(0);
        Class<? extends ProxyMarker> proxyClass = getOrCreateProxyClass(caller, instanceClass);

        // Grab the field type from the parameters list
        Class<?> fieldType = type.parameterType(1);

        // Find appropriate handles
        MethodHandles.Lookup proxyLookup = MethodHandles.privateLookupIn(proxyClass, caller);
        VarHandle initHandle = proxyLookup.findVarHandle(proxyClass, fieldName.concat("$initialized"), boolean.class);
        VarHandle fieldHandle = proxyLookup.findVarHandle(proxyClass, fieldName, fieldType);

        // Generate getProxy handle
        MethodHandle proxyCreationHandle = createHandleForGetProxy(proxyClass, instanceClass);

        // Generate method handle with arguments inserted (so they're cached)
        MethodHandle handle = MethodHandles.insertArguments(SELF_SET_DEFINALIZED_FIELD_HANDLE, 0, proxyCreationHandle, initHandle, fieldHandle);
        return new ConstantCallSite(handle.asType(type));
    }

    // TODO (opt) this handle can be replaced with the proxy field set method handle after first initialization using a MutableCallSite
    public static void setDefinalizedFieldImpl(MethodHandle proxyCreationHandle, VarHandle initHandle, VarHandle fieldHandle, Object instance, Object value) throws Throwable {
        // Get proxy object
        Object proxy = proxyCreationHandle.invoke(instance);

        // Set field and flag
        fieldHandle.set(proxy, value);
        initHandle.set(proxy, true);
    }

    // Called with the same signature as the original method, but as a static
    //
    // Converts the first parameter (instance) to proxy class and invokes target
    public static CallSite invokeProxifiedMethod(MethodHandles.Lookup caller, String name, MethodType type) throws IllegalAccessException, NoSuchMethodException {
        assert type.parameterCount() > 0;

        // Get proxy class based off method type
        Class<?> instanceClass = type.parameterType(0);
        Class<?> proxyClass = getOrCreateProxyClass(caller, instanceClass);

        // Locate target method in proxy
        MethodType targetType = type.dropParameterTypes(0, 1);

        MethodHandle method = MethodHandles.privateLookupIn(proxyClass, caller)
                .findVirtual(proxyClass, name, targetType);

        // Generate a proxy getter handle
        MethodHandle getProxyHandle = createHandleForGetProxy(proxyClass, instanceClass);

        // Bind arguments and return handle
        MethodHandle handle = MethodHandles.insertArguments(SELF_INVOKE_PROXIFIED_METHOD_HANDLE, 0, getProxyHandle, method);
        return new ConstantCallSite(handle
                .asCollector(Object[].class, type.parameterCount() - 1)
                .asType(type));
    }

    public static Object invokeProxifiedMethodImpl(MethodHandle getProxyHandle, MethodHandle target, Object instance, Object[] args) throws Throwable {
        // Get proxy instance
        Object proxy = getProxyHandle.invoke(instance);

        // Invoke target with new instance
        Object[] newArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);

        newArgs[0] = proxy;

        return target.invokeWithArguments(newArgs);
    }

}
