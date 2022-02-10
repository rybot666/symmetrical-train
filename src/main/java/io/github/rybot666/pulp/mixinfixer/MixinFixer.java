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

    private void registerClassHandleType(ClassNode clazz, Type type) {
        boolean isInterface;
        if (type.getSort() == Type.OBJECT) {
            isInterface = fixerContext.isInterface(type.getInternalName());
        } else if (type.getSort() == Type.ARRAY) {
            Type elementType = type.getElementType();
            isInterface = elementType.getSort() == Type.OBJECT && fixerContext.isInterface(elementType.getInternalName());
        } else {
            return;
        }
        if (isInterface) {
            classesUsingInterface.computeIfAbsent(type.getInternalName(), k -> ConcurrentHashMap.newKeySet()).add(clazz.name);
        }
    }

    public void registerClass(ClassNode clazz) {
        if (clazz.interfaces != null) {
            for (String itf : clazz.interfaces) {
                classesImplementingInterface.computeIfAbsent(itf, k -> ConcurrentHashMap.newKeySet()).add(clazz.name);
            }
        }
        if (clazz.methods != null) {
            for (MethodNode method : clazz.methods) {
                Type methodType = Type.getMethodType(method.desc);
                for (Type type : methodType.getArgumentTypes()) {
                    registerClassHandleType(clazz, type);
                }
                registerClassHandleType(clazz, methodType.getReturnType());

                method.accept(new MethodVisitor(ASM.API_VERSION) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        registerClassHandleType(clazz, Type.getObjectType(type));
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        registerClassHandleType(clazz, Type.getObjectType(owner));
                        registerClassHandleType(clazz, Type.getType(descriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        registerClassHandleType(clazz, Type.getObjectType(owner));
                        Type methodType = Type.getMethodType(descriptor);
                        for (Type argType : methodType.getArgumentTypes()) {
                            registerClassHandleType(clazz, argType);
                        }
                        registerClassHandleType(clazz, methodType.getReturnType());
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        Type methodType = Type.getMethodType(descriptor);
                        for (Type argType : methodType.getArgumentTypes()) {
                            registerClassHandleType(clazz, argType);
                        }
                        registerClassHandleType(clazz, methodType.getReturnType());
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        registerClassHandleType(clazz, Type.getType(descriptor));
                    }
                });
            }
        }
    }
}
