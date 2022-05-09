package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import io.github.rybot666.pulp.util.Utils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.WeakHashMap;

public class ProxyFactory {
    public static ClassNode generateBaseProxy(Type target) {
        ClassNode proxy = new ClassNode();
        proxy.name = getProxyClassName(target.getInternalName());
        proxy.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC;
        proxy.superName = "java/lang/Object";
        proxy.version = 60;

        // Add static instances field (map of target instance to their proxy instance)
        Type weakHashType = Type.getType(WeakHashMap.class);

        proxy.fields.add(new FieldNode(
                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "INSTANCES",
                weakHashType.getDescriptor(),
                null,
                null
        ));

        // Debug field
        proxy.fields.add(new FieldNode(
                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                "PROXYING_FOR",
                Type.getDescriptor(String.class),
                null,
                target.getInternalName()
        ));

        // Instance field
        proxy.fields.add(new FieldNode(
                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "this$",
                target.getDescriptor(),
                null,
                null
        ));

        // Static initializer
        proxy.methods.add(Utils.make(new MethodNode(), m -> {
            m.name = "<clinit>";
            m.desc = "()V";
            m.access = Opcodes.ACC_STATIC;

            m.instructions = new InsnList();

            // Initialize weak hash map static
            m.instructions.add(new TypeInsnNode(Opcodes.NEW, weakHashType.getInternalName()));
            m.instructions.add(new InsnNode(Opcodes.DUP));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, weakHashType.getInternalName(), "<init>", "()V"));
            m.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, proxy.name, "INSTANCES", weakHashType.getDescriptor()));

            m.instructions.add(new InsnNode(Opcodes.RETURN));
        }));

        // General constructor
        proxy.methods.add(Utils.make(new MethodNode(), m -> {
            m.name = "<init>";
            m.desc = Type.getMethodDescriptor(Type.VOID_TYPE, target);
            m.access = Opcodes.ACC_PUBLIC;

            m.instructions = new InsnList();

            // Object super call
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(Object.class),
                    "<init>",
                    "()V"
            ));

            // Set instance field
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));

            m.instructions.add(new FieldInsnNode(
                    Opcodes.PUTFIELD,
                    proxy.name,
                    "this$",
                    target.getDescriptor()
            ));

            m.instructions.add(new InsnNode(Opcodes.RETURN));
        }));

        // Apply proxy marker interface
        proxy.interfaces.add(Type.getInternalName(ProxyMarker.class));

        return proxy;
    }

    public static String getProxyClassName(String name) {
        String packageName = name.replace("/", "$$");

        return "io/github/rybot666/pulp/proxies/".concat(packageName);
    }

    private ProxyFactory() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }
}
