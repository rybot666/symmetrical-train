package io.github.rybot666.pulp.mixin_backend.transformer.transformations;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.IndyFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

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

        node.instructions.add(IndyFactory.getProxyIndy(originalClass));

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
        node.instructions.add(new InsnNode(desc.getReturnType().getOpcode(Opcodes.IRETURN)));

        return node;
    }

    private static void transformAddedMethod(ClassNode targetClass, Type proxyClass, MethodNode addedMethod) {
        // 1. Usages of non-public fields
        //
        // This is an issue because Mixin assumes that the injected method is inside the target class. Normally this is
        // true, so all fields are accessible to the injected method. However, we moved the method to a proxy class, so
        // those fields must be accessed reflectively (we use indy)

        for (AbstractInsnNode insn: addedMethod.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

                if (!fieldInsnNode.owner.equals(targetClass.name)) {
                    continue;
                }

                // Locate field in target
                FieldNode targetField = targetClass.fields.stream()
                        .filter(f -> f.name.equals(fieldInsnNode.name) && f.desc.equals(fieldInsnNode.desc))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Field instruction trying to access nonexistent field"));

                // Skip public fields (perf)
                if ((targetField.access & Opcodes.ACC_PUBLIC) != 0) continue;

                Type ownerType = Type.getObjectType(fieldInsnNode.owner);
                Type fieldType = Type.getType(fieldInsnNode.desc);

                InsnList insnList = new InsnList();

                switch (fieldInsnNode.getOpcode()) {
                    case Opcodes.GETFIELD:
                        // The ALOAD 0 is already before the field insn
                        // insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));

                        insnList.add(new FieldInsnNode(
                                Opcodes.GETFIELD,
                                proxyClass.getInternalName(),
                                "this$",
                                Type.getObjectType(targetClass.name).getDescriptor())
                        );
                        insnList.add(IndyFactory.getPrivateField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    case Opcodes.PUTFIELD:
                        // The ALOAD 0 is already before the field insn
                        // insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));

                        insnList.add(new FieldInsnNode(
                                Opcodes.GETFIELD,
                                proxyClass.getInternalName(),
                                "this$",
                                Type.getObjectType(targetClass.name).getDescriptor())
                        );
                        insnList.add(IndyFactory.setPrivateField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    case Opcodes.GETSTATIC:
                        //indyNode = IndyFactory.getStaticField(ownerType, fieldInsnNode.name, fieldType);

                        insnList.add(IndyFactory.getStaticField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    case Opcodes.PUTSTATIC:
                        insnList.add(IndyFactory.setStaticField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown field insn opcode: " + fieldInsnNode.getOpcode());
                }

                addedMethod.instructions.insertBefore(fieldInsnNode, insnList);
                addedMethod.instructions.remove(fieldInsnNode);
            }
        }
    }

    @Override
    public boolean apply(TransformationState state) {
        // Transform methods in the original class to refer to the new class
        List<String> targetMethods = new ArrayList<>();

        Type targetType = Type.getObjectType(state.transformed.name);
        Type proxyType = Type.getObjectType(state.proxy.name);

        for (MethodNode addedMethod : state.addedMethods) {
            // Remove all added methods from the original class
            state.transformed.methods.remove(addedMethod);

            // Transform them as needed and move them to the proxy class
            transformAddedMethod(state.transformed, proxyType, addedMethod);
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
                        methodInsnNode.owner = state.proxy.name;

                        if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                            AbstractInsnNode previous = methodInsnNode.getPrevious();
                            method.instructions.remove(methodInsnNode);

                            methodInsnNode.setOpcode(Opcodes.INVOKEVIRTUAL);

                            MethodNode thunk = generateStaticProxyThunk(targetType, methodInsnNode);
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
