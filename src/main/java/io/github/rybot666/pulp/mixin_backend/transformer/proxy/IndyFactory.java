package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class IndyFactory {
    private static final String BSM_INTERNAL = Type.getInternalName(PulpBootstrapMethods.class);
    private static final Method GET_PROXY_METHOD;

    static {
        try {
            GET_PROXY_METHOD = PulpBootstrapMethods.class.getDeclaredMethod("getProxy", MethodHandles.Lookup.class, String.class, MethodType.class);
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
                "getProxy",
                Type.getMethodDescriptor(GET_PROXY_METHOD),
                false
        );

        return new InvokeDynamicInsnNode(
                "getProxy",
                Type.getMethodDescriptor(proxyClazz, targetClazz),
                handle
        );
    }
}
