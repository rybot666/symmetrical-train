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
    private static final Method GET_DEFINALIZED_FIELD_METHOD;
    private static final Method SET_DEFINALIZED_FIELD_METHOD;
    private static final Method INVOKE_PROXIFIED_METHOD_METHOD;

    static {
        try {
            GET_PROXY_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getProxy", MethodHandles.Lookup.class, MethodType.class, String.class);

            GET_PRIVATE_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getPrivateField", MethodHandles.Lookup.class, String.class, MethodType.class);
            SET_PRIVATE_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("setPrivateField", MethodHandles.Lookup.class, String.class, MethodType.class);
            GET_STATIC_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getStaticField", MethodHandles.Lookup.class, String.class, MethodType.class, Class.class);
            SET_STATIC_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("setStaticField", MethodHandles.Lookup.class, String.class, MethodType.class, Class.class);

            GET_DEFINALIZED_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getDefinalizedField", MethodHandles.Lookup.class, String.class, MethodType.class);
            SET_DEFINALIZED_FIELD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("setDefinalizedField", MethodHandles.Lookup.class, String.class, MethodType.class);

            INVOKE_PROXIFIED_METHOD_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("invokeProxifiedMethod", MethodHandles.Lookup.class, String.class, MethodType.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private IndyFactory() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static InvokeDynamicInsnNode getProxy(Type targetClazz) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                GET_PROXY_METHOD.getName(),
                Type.getMethodDescriptor(GET_PROXY_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                "proxy",
                Type.getMethodDescriptor(Type.getObjectType("java/lang/Object"), targetClazz),
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
                Type.getMethodDescriptor(Type.VOID_TYPE, target, fieldType),
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

    public static InvokeDynamicInsnNode getDefinalizedField(Type originalClass, String fieldName, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                GET_DEFINALIZED_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(GET_DEFINALIZED_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                fieldName,
                Type.getMethodDescriptor(fieldType, originalClass),
                handle
        );
    }

    public static InvokeDynamicInsnNode setDefinalizedField(Type instanceType, String fieldName, Type fieldType) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                SET_DEFINALIZED_FIELD_METHOD.getName(),
                Type.getMethodDescriptor(SET_DEFINALIZED_FIELD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                fieldName,
                Type.getMethodDescriptor(Type.VOID_TYPE, instanceType, fieldType),
                handle
        );
    }

    public static InvokeDynamicInsnNode invokeProxifiedMethod(String name, String descriptor) {
        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                BSM_INTERNAL,
                INVOKE_PROXIFIED_METHOD_METHOD.getName(),
                Type.getMethodDescriptor(INVOKE_PROXIFIED_METHOD_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                name,
                descriptor,
                handle
        );
    }
}
