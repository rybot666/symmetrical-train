package io.github.rybot666.pulp.mixin_backend.transformer.remap;

import io.github.rybot666.pulp.util.MemberInfo;
import io.github.rybot666.pulp.util.Utils;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.logging.Level;

/**
 * Stores information on all classes Pulp has seen
 *
 * This includes usages of fields and interfaces, as these need to be remapped at runtime
 */
public class MassClassState {
    private static final PulpLogger LOGGER = LogUtils.getLogger("Class State");

    /**
     * Map of field descriptors to a list of class internal names using that field
     */
    public final Map<MemberInfo, Set<String>> fieldUsages = new HashMap<>();

    /**
     * Map of class internal names to a list of classes using them
     */
    public final Map<Type, Set<String>> typeUsages = new HashMap<>();

    private void registerUsage(Type type, String user) {
        this.typeUsages.computeIfAbsent(type, k -> new HashSet<>()).add(user);
    }

    private void registerMethodArgsAndReturn(Type methodType, String user) {
        this.registerUsage(methodType.getReturnType(), user);

        for (Type argType: methodType.getArgumentTypes()) {
            this.registerUsage(argType, user);
        }
    }

    /**
     * Registers all field and interface usages in the given class
     */
    public void process(ClassNode node) {
        // Superclass
        if (node.superName != null) {
            this.registerUsage(Type.getObjectType(node.superName), node.name);
        }

        // Interfaces
        for (String iface: node.interfaces) {
            this.registerUsage(Type.getObjectType(iface), node.name);
        }

        // Types of instance fields
        for (FieldNode field: node.fields) {
            Type type = Type.getType(field.desc);
            this.registerUsage(type, node.name);
        }

        // Types and fields used in methods
        for (MethodNode method: node.methods) {
            this.registerMethodArgsAndReturn(Type.getMethodType(method.desc), node.name);

            for (AbstractInsnNode insn: method.instructions) {
                if (insn instanceof TypeInsnNode) {
                    TypeInsnNode typeInsn = (TypeInsnNode) insn;
                    this.registerUsage(Type.getObjectType(typeInsn.desc), node.name);
                } else if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;

                    this.registerUsage(Type.getObjectType(fieldInsn.owner), node.name);
                    this.registerUsage(Type.getType(fieldInsn.desc), node.name);

                    this.fieldUsages.computeIfAbsent(MemberInfo.from(fieldInsn), k -> new HashSet<>()).add(node.name);
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;

                    this.registerUsage(Type.getObjectType(methodInsn.owner), node.name);
                    this.registerMethodArgsAndReturn(Type.getMethodType(methodInsn.desc), node.name);
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indyInsn = (InvokeDynamicInsnNode) insn;
                    this.registerMethodArgsAndReturn(Type.getMethodType(indyInsn.desc), node.name);
                } else if (insn instanceof MultiANewArrayInsnNode) {
                    MultiANewArrayInsnNode manaInsn = (MultiANewArrayInsnNode) insn;
                    this.registerUsage(Type.getType(manaInsn.desc), node.name);
                }
            }
        }
    }

    public void registerAllClasses(ClassLoader loader, Instrumentation instrumentation) {
        long start = System.currentTimeMillis();

        Class<?>[] allLoaded = instrumentation.getAllLoadedClasses();
        LOGGER.info(() -> String.format("Searching %d classes for required class state...", allLoaded.length));

        int invalidCount = 0;

        for (Class<?> clazz : allLoaded) {
            try {
                if (clazz.isSynthetic() || clazz.isPrimitive()) {
                    continue;
                }

                if (clazz.isArray()) {
                    clazz = clazz.getComponentType();
                }

                ClassReader reader = Utils.getClassReader(loader, clazz.getName());

                if (reader == null) {
                    invalidCount++;
                    continue;
                }

                ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_DEBUG);

                this.process(node);
            } catch (ClassNotFoundException ignored) {

            } catch (IOException e) {
                Class<?> finalClazz = clazz;
                LOGGER.log(Level.WARNING, e, () -> String.format("IO Exception while reading class %s", finalClazz.getName()));
            }
        }

        long diff = System.currentTimeMillis() - start;

        int finalInvalidCount = invalidCount;
        LOGGER.info(() -> String.format("Searched %d classes (%d missing bytecode) in %dms",
                allLoaded.length - finalInvalidCount, finalInvalidCount, diff));
    }
}
