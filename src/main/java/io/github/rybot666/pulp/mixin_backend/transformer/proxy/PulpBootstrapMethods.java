package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import io.github.rybot666.pulp.util.log.LogUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * Bootstrap methods used by generated classes (invokedynamic)
 */
public class PulpBootstrapMethods {
    private static final Logger LOGGER = LogUtils.getLogger("Indy");
    private static final String SELF_INTERNAL = Type.getInternalName(PulpBootstrapMethods.class);
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
    @SuppressWarnings({"unchecked", "unused"}) // Used reflectively
    public static CallSite getProxy(MethodHandles.Lookup caller, String name, MethodType type, String targetClazz) {
        Class<? extends ProxyMarker> proxyClazz = ProxyFactory.PROXY_CLASS_MAP.get(targetClazz);

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

    public static InvokeDynamicInsnNode generateGetProxyNode(Type targetClazz) {
        Type proxyClazz = Type.getObjectType(ProxyFactory.getProxyClassName(targetClazz.getInternalName()));

        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                SELF_INTERNAL,
                "getProxy",
                Type.getMethodDescriptor(
                        Type.getType(CallSite.class),
                        Type.getType(MethodHandles.Lookup.class),
                        Type.getType(String.class),
                        Type.getType(MethodType.class),
                        Type.getType(String.class)
                ),
                false
        );

        return new InvokeDynamicInsnNode(
                "getProxy",
                Type.getMethodDescriptor(proxyClazz, targetClazz),
                handle,
                targetClazz.getInternalName()
        );
    }
}
