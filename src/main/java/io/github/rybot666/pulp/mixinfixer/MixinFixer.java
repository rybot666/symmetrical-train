package io.github.rybot666.pulp.mixinfixer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.asm.ASM;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MixinFixer {
    private final IMixinFixerContext fixerContext;
    private final Map<String, Set<String>> classesImplementingInterface = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> classesUsingInterface = new ConcurrentHashMap<>();

    public MixinFixer(IMixinFixerContext fixerContext) {
        this.fixerContext = fixerContext;
    }

    public void registerClass(ClassNode clazz) {
        if (clazz.interfaces != null) {
            for (String itf : clazz.interfaces) {
                classesImplementingInterface.computeIfAbsent(itf, k -> ConcurrentHashMap.newKeySet()).add(clazz.name);
            }
        }
        if (clazz.methods != null) {
            for (MethodNode method : clazz.methods) {
                method.accept(new MethodVisitor(ASM.API_VERSION) {
                    private void handleType(Type type) {
                        boolean isInterface;
                        if (type.getSort() == Type.OBJECT) {
                            isInterface = fixerContext.isInterface(type.getInternalName());
                        } else if (type.getSort() == Type.ARRAY) {
                            isInterface = fixerContext.isInterface(type.getElementType().getInternalName());
                        } else {
                            return;
                        }
                        if (isInterface) {
                            classesUsingInterface.computeIfAbsent(type.getInternalName(), k -> ConcurrentHashMap.newKeySet()).add(clazz.name);
                        }
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        handleType(Type.getObjectType(type));
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        handleType(Type.getObjectType(owner));
                        handleType(Type.getType(descriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        handleType(Type.getObjectType(owner));
                        Type methodType = Type.getMethodType(descriptor);
                        for (Type argType : methodType.getArgumentTypes()) {
                            handleType(argType);
                        }
                        handleType(methodType.getReturnType());
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        Type methodType = Type.getMethodType(descriptor);
                        for (Type argType : methodType.getArgumentTypes()) {
                            handleType(argType);
                        }
                        handleType(methodType.getReturnType());
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        handleType(Type.getType(descriptor));
                    }
                });
            }
        }
    }
}
