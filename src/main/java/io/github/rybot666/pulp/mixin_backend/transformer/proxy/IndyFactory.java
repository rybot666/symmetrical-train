package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class IndyFactory {
    private static final String BSM_INTERNAL = Type.getInternalName(PulpBootstrapMethods.class);
    private static final Method GET_PROXY_METHOD;
    private static final Method GET_PRIVATE_FIELD_METHOD;
    private static final Method SET_PRIVATE_FIELD_METHOD;
    private static final Method GET_STATIC_FIELD_METHOD;
    private static final Method SET_STATIC_FIELD_METHOD;

    static {
        try {
            GET_PROXY_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getProxy", MethodHandles.Lookup.class, String.class, MethodType.class);

            GET_PRIVATE_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getPrivateField", MethodHandles.Lookup.class, String.class, MethodType.class);
            SET_PRIVATE_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("setPrivateField", MethodHandles.Lookup.class, String.class, MethodType.class);
            GET_STATIC_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getStaticField", MethodHandles.Lookup.class, String.class, MethodType.class, Class.class);
            SET_STATIC_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("setStaticField", MethodHandles.Lookup.class, String.class, MethodType.class, Class.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private IndyFactory() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static InvokeDynamicInsnNode getProxyIndy(Type targetClazz) {
        Type proxyClazz = Type.getObjectType(ProxyFactory.getProxyClassName(targetClazz.getInternalName()));

        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                GET_PROXY_METHOD.getName(),
                Type.getMethodDescriptor(GET_PROXY_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                "proxy",
                Type.getMethodDescriptor(proxyClazz, targetClazz),
                handle
        );
    }

    public static InvokeDynamicInsnNode getPrivateField(Type target, String name, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                GET_PRIVATE_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(GET_PRIVATE_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                name,
                Type.getMethodDescriptor(fieldType, target),
                handle
        );
    }

    public static InvokeDynamicInsnNode setPrivateField(Type target, String name, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                SET_PRIVATE_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(SET_PRIVATE_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                name,
                Type.getMethodDescriptor(Type.VOID_TYPE, fieldType, target),
                handle
        );
    }

    public static InvokeDynamicInsnNode getStaticField(Type target, String name, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                GET_STATIC_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(GET_STATIC_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                name,
                Type.getMethodDescriptor(fieldType),
                handle,
                target
        );
    }

    public static InvokeDynamicInsnNode setStaticField(Type target, String name, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                SET_STATIC_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(SET_STATIC_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                name,
                Type.getMethodDescriptor(Type.VOID_TYPE, fieldType),
                handle,
                target
        );
    }
}
