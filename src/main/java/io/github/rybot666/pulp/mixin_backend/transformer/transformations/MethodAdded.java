package io.github.rybot666.pulp.mixin_backend.transformer.transformations;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Transformation used when a method is added to a class
 */
public class MethodAdded extends Transformation {
    @Override
    public boolean apply(TransformationState state) {
        // - Transform methods in the original class to refer to the new class

        Map<String, MethodNode> methodMap = new HashMap<>();

        for (MethodNode addedMethod : state.addedMethods) {
            // Remove all added methods from the original class
            state.transformed.methods.remove(addedMethod);

            // Move them to the proxy class
            state.proxy.methods.add(addedMethod);

            // Store method in map for transformation step
            methodMap.put(addedMethod.name + ";" + addedMethod.desc, addedMethod);
        }

        for (MethodNode method : state.transformed.methods) {
            if (method.instructions == null) continue;

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                    MethodNode mapValue = methodMap.get(methodInsnNode.name + ";" + methodInsnNode.desc);

                    if (mapValue == null) continue;

                    methodInsnNode.name = mapValue.name;
                    methodInsnNode.desc = mapValue.desc;
                }
            }
        }

        return true;
    }
}
