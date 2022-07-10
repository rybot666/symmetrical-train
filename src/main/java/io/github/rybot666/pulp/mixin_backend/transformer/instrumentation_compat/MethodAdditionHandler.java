package io.github.rybot666.pulp.mixin_backend.transformer.instrumentation_compat;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.IndyFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles mixins that add methods to the target (e.g. injects and methods that were not in the original class)
 */
// TODO this only works for methods that aren't called from outside of the target (e.g. injects but not added methods)
//     (requires a new global transform)
public class MethodAdditionHandler {
    // TODO (perf) replace this with an indy method
    /**
     * Generates a static method that pulls the proxy node for the first argument (the instance) and then forwards the
     * method call to the proxy with the remaining arguments
     *
     * The target method call node must not be in an insn list or bad things *will* happen
     */
    private static MethodNode generateStaticProxyThunk(Type originalClass, MethodInsnNode targetMethodCall) {
        Type originalDesc = Type.getMethodType(targetMethodCall.desc);

        // Add the original class as the first argument to the static (takes the implicit `this` argument)
        Type[] arguments = new Type[originalDesc.getArgumentTypes().length + 1];
        System.arraycopy(originalDesc.getArgumentTypes(), 0, arguments, 1, originalDesc.getArgumentTypes().length);

        arguments[0] = originalClass;

        Type desc = Type.getMethodType(originalDesc.getReturnType(), arguments);

        MethodNode node = new MethodNode();

        node.desc = desc.getDescriptor();
        node.name = targetMethodCall.name.concat("$staticThunk");
        node.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;

        node.instructions = new InsnList();

        // Pull proxy class for target
        int slot = 0;

        node.instructions.add(new VarInsnNode(originalClass.getOpcode(Opcodes.ILOAD), 0));
        slot += originalClass.getSize();

        node.instructions.add(IndyFactory.getProxy(originalClass));

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


    // TODO(hack) the swaps here are ew. Ideally we could find a better way to get the instance
    /**
     * Handles transformation on added methods to support their movement to a proxy class
     *
     * 1) Uses indy for access on private fields of the instance class (so the proxy can access them)
     */
    private static void transformForProxy(ClassNode targetClass, Type proxyType, MethodNode addedMethod) {
        for (AbstractInsnNode insn: addedMethod.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

                if (!fieldInsnNode.owner.equals(targetClass.name)) {
                    continue;
                }

                /*
                // Locate field in target
                FieldNode targetField = targetClass.fields.stream()
                        .filter(f -> f.name.equals(fieldInsnNode.name) && f.desc.equals(fieldInsnNode.desc))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Field instruction trying to access nonexistent field"));

                // Skip public fields (perf)
                if ((targetField.access & Opcodes.ACC_PUBLIC) != 0) continue;
                */

                Type ownerType = Type.getObjectType(fieldInsnNode.owner);
                Type fieldType = Type.getType(fieldInsnNode.desc);

                InsnList insnList = new InsnList();

                switch (fieldInsnNode.getOpcode()) {
                    case Opcodes.GETFIELD:
                        insnList.add(IndyFactory.getPrivateField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    case Opcodes.PUTFIELD:
                        insnList.add(IndyFactory.setPrivateField(ownerType, fieldInsnNode.name, fieldType));

                        break;

                    case Opcodes.GETSTATIC:
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

            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insn;

                // Handle ALOAD 0 with a get on the instance field
                if (varInsnNode.getOpcode() == Opcodes.ALOAD && varInsnNode.var == 0) {
                    FieldInsnNode instanceGetInsn = new FieldInsnNode(
                            Opcodes.GETFIELD,
                            proxyType.getInternalName(),
                            "this$",
                            Type.getObjectType(targetClass.name).getDescriptor()
                    );

                    addedMethod.instructions.insert(varInsnNode, instanceGetInsn);
                }
            }
        }
    }

    public static boolean handle(ClassNode untransformed, ClassNode transformed, ClassNode proxy) {
        boolean modifiedProxy = false;

        // Transform methods in the original class to refer to the proxy class
        Type targetType = Type.getObjectType(transformed.name);
        Type proxyType = Type.getObjectType(proxy.name);

        // We only care about methods that were added to the target
        Set<MethodNode> addedMethodNodes = new HashSet<>(transformed.methods);

        for (MethodNode method: untransformed.methods) {
            addedMethodNodes.removeIf(m -> m.name.equals(method.name) && m.desc.equals(method.desc));
        }

        // Move all added methods to the proxy
        Set<String> addedMethods = new HashSet<>();

        for (MethodNode method: addedMethodNodes) {
            transformed.methods.remove(method);

            // Transform so they work in proxy context
            transformForProxy(transformed, proxyType, method);
            proxy.methods.add(method);

            modifiedProxy = true;

            // Store name and descriptor
            addedMethods.add(method.name + method.desc);
        }

        // Do a pass over all transformed methods to replace call sites
        for (MethodNode method : transformed.methods) {
            if (method.instructions == null) continue;

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

                    // If the call is against an added method
                    if (methodInsnNode.owner.equals(transformed.name) && addedMethods.contains(methodInsnNode.name + methodInsnNode.desc)) {
                        // If the call was an internal call
                        if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) {
                            // Convert to an indy

                            // Add the original class as the first argument
                            Type originalDesc = Type.getMethodType(methodInsnNode.desc);

                            Type[] arguments = new Type[originalDesc.getArgumentTypes().length + 1];
                            System.arraycopy(originalDesc.getArgumentTypes(), 0, arguments, 1, originalDesc.getArgumentTypes().length);

                            arguments[0] = Type.getObjectType(methodInsnNode.owner);

                            // Recreate method descriptor
                            Type newType = Type.getMethodType(originalDesc.getReturnType(), arguments);

                            // And pass to indy to generate proxy and passthrough
                            InvokeDynamicInsnNode indyNode = IndyFactory.invokeProxifiedMethod(
                                    methodInsnNode.name,
                                    newType.getDescriptor()
                            );

                            method.instructions.set(methodInsnNode, indyNode);
                        } else {
                            // Otherwise just call against proxy class
                            // TODO this will crash because the class doesn't exist yet - replace with proxy class generation
                            methodInsnNode.owner = proxy.name;
                        }
                    }
                }
            }
        }

        return modifiedProxy;
    }
}
