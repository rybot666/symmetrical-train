package io.github.rybot666.pulp.mixin_backend.transformer.proxy;

import io.github.rybot666.pulp.util.Util;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.WeakHashMap;

public class ProxyFactory {
    // Map of internal target name to proxy classes
    public static final WeakHashMap<String, Class<? extends ProxyMarker>> PROXY_CLASS_MAP = new WeakHashMap<>();

    public static ClassNode generateBaseProxy(ClassNode target) {
        ClassNode proxy = new ClassNode();
        proxy.name = getProxyClassName(target.name);
        proxy.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC;
        proxy.superName = "java/lang/Object";

        // Add static instances field (map of target instance to their proxy instance)
        Type weakHashType = Type.getType(WeakHashMap.class);

        proxy.fields.add(new FieldNode(
                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "INSTANCES",
                weakHashType.getDescriptor(),
                null,
                null
        ));

        // Static initializer
        proxy.methods.add(Util.make(new MethodNode(), m -> {
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

        // Apply proxy marker interface
        proxy.interfaces.add(Type.getDescriptor(ProxyMarker.class));

        return proxy;
    }

    public static String getProxyClassName(String name) {
        String packageName = "";
        String className = name;

        int idx = name.lastIndexOf('/');
        if (idx != -1) {
            className = name.substring(idx + 1);
            packageName = name.substring(0, idx);
        }

        return packageName.concat("/").concat("$$PulpProxy$").concat(className);
    }
}
