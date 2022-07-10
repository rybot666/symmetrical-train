package io.github.rybot666.pulp.mixin_backend.transformer.remap;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.IndyFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

// TODO(opt) definalised fields convert proxy to instance and back unnecessarily. Can be elided

/**
 * Handles global remapping of definalized fields
 */
public class FieldDefinalizationRemapper {
    /**
     * Map containing remaps applied to fields
     *
     * Key is the original field (in owner;name:desc) form, value is the new owner
     */
    private final Map<String, String> fields = new HashMap<>();

    public void register(String originalOwner, String name, String desc, String newOwner) {
        String key = originalOwner + ";" + name + ":" + desc;
        this.fields.put(key, newOwner);
    }

    /**
     * Remaps all field usages in the given class
     */
    public boolean remap(ClassNode node) {
        boolean modified = false;

        for (MethodNode method : node.methods) {
            modified |= this.remapMethod(method);
        }

        return modified;
    }

    private boolean remapMethod(MethodNode method) {
        boolean modified = false;

        for (AbstractInsnNode insn: method.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;

                // If the field is remapped to a new owner, set the new owner
                String key = fieldInsn.owner + ";" + fieldInsn.name + ":" + fieldInsn.desc;
                String newOwner = this.fields.get(key);

                if (newOwner == null) {
                    continue;
                }

                modified = true;

                InvokeDynamicInsnNode indyNode;

                if (fieldInsn.getOpcode() == Opcodes.GETFIELD || fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
                    // Get operations need an indy to possibly copy the old value
                    indyNode = IndyFactory.getDefinalizedField(
                            Type.getObjectType(fieldInsn.owner),
                            fieldInsn.name,
                            Type.getType(fieldInsn.desc)
                    );
                } else {
                    // Set operations need an indy to set the init flag
                    indyNode = IndyFactory.setDefinalizedField(
                            Type.getObjectType(fieldInsn.owner),
                            fieldInsn.name,
                            Type.getType(fieldInsn.desc)
                    );
                }

                method.instructions.set(fieldInsn, indyNode);
            }
        }

        return modified;
    }
}
