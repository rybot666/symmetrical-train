package io.github.rybot666.pulp.mixin_backend.transformer;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.IndyFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ProxiedFieldRemapper {
    // Set of all remapped fields
    private final Set<String> remappedFields = new HashSet<>();

    // Map of classes that have remapped fields to the proxy classes for them
    private final Map<String, Type> proxyClassMap = new HashMap<>();

    // Map of `className;fieldName` to a set of classes that refer to that field
    private final Map<String, Set<String>> fieldUsages = new HashMap<>();

    // Set of classes already scanned for their fields
    private final Set<String> processedClasses = new HashSet<>();

    public void scanClassForUsages(ClassNode node) {
        if (this.processedClasses.contains(node.name)) {
            return;
        }

        this.processedClasses.add(node.name);

        for (MethodNode method: node.methods) {
            if (method.instructions == null) {
                continue;
            }

            for (AbstractInsnNode insn: method.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

                    this.fieldUsages
                            .computeIfAbsent(fieldInsnNode.owner.concat(fieldInsnNode.name), k -> new HashSet<>())
                            .add(node.name);
                }
            }
        }
    }

    /**
     * Remaps field usages within the provided class node
     *
     * @param node the target node
     */
    public void remap(ClassNode node) {
        // TODO handle static fields

        for (MethodNode method: node.methods) {
            if (method.instructions == null) {
                continue;
            }

            for (AbstractInsnNode insn: method.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

                    if (insn.getOpcode() != Opcodes.GETFIELD) {
                        continue;
                    }

                    String remapKey = fieldInsnNode.owner.concat(";").concat(fieldInsnNode.name);

                    if (!this.remappedFields.contains(remapKey)) {
                        continue;
                    }

                    Type proxyClass = this.proxyClassMap.get(fieldInsnNode.owner);
                    assert proxyClass != null;

                    InvokeDynamicInsnNode indyNode = IndyFactory.getDefinalizedField(
                            Type.getObjectType(fieldInsnNode.owner),
                            fieldInsnNode.name,
                            Type.getType(fieldInsnNode.desc)
                    );

                    fieldInsnNode.owner = proxyClass.getInternalName();

                    method.instructions.set(fieldInsnNode, indyNode);
                }
            }
        }
    }

    public void addFieldRemap(String className, Type proxyClass, String fieldName) {
        this.proxyClassMap.put(className, proxyClass);
        this.remappedFields.add(className.concat(";").concat(fieldName));
    }

    public boolean needsRemap(String className, String fieldName) {
        return this.remappedFields.contains(className.concat(";").concat(fieldName));
    }
}
