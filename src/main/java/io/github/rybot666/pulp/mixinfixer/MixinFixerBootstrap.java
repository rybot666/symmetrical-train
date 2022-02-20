package io.github.rybot666.pulp.mixinfixer;

import com.google.common.collect.MapMaker;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public class MixinFixerBootstrap {
    public static final ConcurrentMap<Object, Object> PROXY_OBJECTS = new MapMaker().weakKeys().makeMap();

    public static Object proxy(Object object) {
        return PROXY_OBJECTS.getOrDefault(object, object);
    }

    public static CallSite proxyBootstrap(MethodHandles.Lookup caller, String name, MethodType type) throws Throwable {
        return new ConstantCallSite(caller.findStatic(MixinFixerBootstrap.class, "proxy", MethodType.methodType(Object.class, Object.class)).asType(type));
    }

    static InvokeDynamicInsnNode callProxy(String returnType) {
        Handle proxyBootstrap = new Handle(
                Opcodes.H_INVOKESTATIC,
                "io/github/rybot666/pulp/mixinfixer/MixinFixerBootstrap",
                "proxyBootstrap",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
        );
        return new InvokeDynamicInsnNode("proxy", "(Ljava/lang/Object;)" + returnType, proxyBootstrap);
    }
}
