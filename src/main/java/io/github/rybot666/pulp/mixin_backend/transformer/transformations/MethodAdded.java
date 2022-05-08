package io.github.rybot666.pulp.mixin_backend.transformer.transformations;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.PulpBootstrapMethods;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.asm.ASM;

import java.util.*;

/**
 * Transformation used when a method is added to a class
 */
public class MethodAdded extends Transformation {
    // The target method call node must not be in an insn list or bad things *will* happen
    private static MethodNode generateStaticProxyThunk(Type originalClass, MethodInsnNode targetMethodCall) {
        MethodNode node = new MethodNode();

        Type originalDesc = Type.getMethodType(targetMethodCall.desc);

        Type[] arguments = new Type[originalDesc.getArgumentTypes().length + 1];
        System.arraycopy(originalDesc.getArgumentTypes(), 0, arguments, 1, originalDesc.getArgumentTypes().length);

        arguments[0] = originalClass;

        Type desc = Type.getMethodType(originalDesc.getReturnType(), arguments);

        node.desc = desc.getDescriptor();
        node.name = targetMethodCall.name.concat("$staticThunk");
        node.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;

        node.instructions = new InsnList();

        // Pull proxy class for target
        int slot = 0;

        node.instructions.add(new VarInsnNode(originalClass.getOpcode(Opcodes.ILOAD), 0));
        slot += originalClass.getSize();

        node.instructions.add(PulpBootstrapMethods.generateGetProxyNode(originalClass));

        // Load arguments from LVT on to stack
        // Skip the first one because it's the target instance
        for (int i = 1; i < desc.getArgumentTypes().length; i++) {
            Type argument = desc.getArgumentTypes()[i];

            int opcode = argument.getOpcode(Opcodes.ILOAD);
            node.instructions.add(new VarInsnNode(opcode, slot));

            slot += argument.getSize();
        }

        // Invoke target method
        node.instructions.add(targetMethodCall);

        // Proxy return
        int opcode = desc.getReturnType().getOpcode(Opcodes.IRETURN);
        InsnNode insn = new InsnNode(opcode);
        node.instructions.add(insn);

        return node;
    }

    @Override
    public boolean apply(TransformationState state) {
        // Transform methods in the original class to refer to the new class
        List<String> targetMethods = new ArrayList<>();

        for (MethodNode addedMethod : state.addedMethods) {
            // Remove all added methods from the original class
            state.transformed.methods.remove(addedMethod);

            // Move them to the proxy class
            state.proxy.methods.add(addedMethod);

            // Store method in map for transformation step
            targetMethods.add(addedMethod.name + addedMethod.desc);
        }

        for (MethodNode method : state.transformed.methods) {
            if (method.instructions == null) continue;

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

                    if (methodInsnNode.owner.equals(state.transformed.name) && targetMethods.contains(methodInsnNode.name + methodInsnNode.desc)) {
                        Type original = Type.getObjectType(methodInsnNode.owner);

                        methodInsnNode.owner = state.proxy.name;

                        if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                            AbstractInsnNode previous = methodInsnNode.getPrevious();
                            method.instructions.remove(methodInsnNode);

                            methodInsnNode.setOpcode(Opcodes.INVOKEVIRTUAL);

                            MethodNode thunk = generateStaticProxyThunk(original, methodInsnNode);
                            state.proxy.methods.add(thunk);

                            MethodInsnNode thunkNode = new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    state.proxy.name,
                                    thunk.name,
                                    thunk.desc
                            );

                            method.instructions.insert(previous, thunkNode);
                        }
                    }
                }
            }
        }

        return true;
    }
}
