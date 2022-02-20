package io.github.rybot666.pulp.mixinfixer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.spongepowered.asm.util.asm.ASM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MixinFixer {
    private static final String PROXY_CLASS_FORMAT = "io/github/rybot666/pulp/proxy/%s$Proxy$";
    private static final String PROXY_FIELD = "$$$proxy$$$";

    private final IMixinFixerContext fixerContext;
    private final Map<String, Set<String>> classesImplementingInterface = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> classesUsingInterface = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isInterfaceCache = new ConcurrentHashMap<>();

    public MixinFixer(IMixinFixerContext fixerContext) {
        this.fixerContext = fixerContext;
    }

    private boolean isInterface(String internalName) {
        if (this.isInterfaceCache.containsKey(internalName)) {
            return this.isInterfaceCache.get(internalName);
        }

        boolean isInterface = this.fixerContext.isInterface(internalName);
        this.isInterfaceCache.put(internalName, isInterface);

        return isInterface;
    }

    private void registerClassHandleType(ClassNode clazz, Type type) {
        boolean isInterface;
        if (type.getSort() == Type.OBJECT) {
            isInterface = this.isInterface(type.getInternalName());
        } else if (type.getSort() == Type.ARRAY) {
            Type elementType = type.getElementType();
            isInterface = elementType.getSort() == Type.OBJECT && this.isInterface(elementType.getInternalName());
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

    public void fixupClass(ClassNode clazz) {

    }

    public void splitClass(ClassNode untransformed, ClassNode transformed) {
        ClassNode proxyClass = makeProxyClass(untransformed.name);
        boolean needsProxyClass = splitInterfaces(untransformed, transformed, proxyClass);
        needsProxyClass |= splitFields(untransformed, transformed, proxyClass);
        needsProxyClass |= splitMethods(untransformed, transformed, proxyClass);

        if (needsProxyClass) {
            fixupMoves(transformed, proxyClass, () -> MixinFixerBootstrap.callProxy(proxyClass.name));
            fixupMoves(proxyClass, transformed, () -> new FieldInsnNode(Opcodes.GETFIELD, proxyClass.name, PROXY_FIELD, "L" + transformed.name + ";"));
        }

        if (needsProxyClass) {
            fixerContext.defineProxyClass(proxyClass);
        }
    }

    private static ClassNode makeProxyClass(String vanillaClassName) {
        String vanillaClassDesc = "L" + vanillaClassName + ";";
        ClassNode proxyClass = new ClassNode();
        proxyClass.access = Opcodes.ACC_PUBLIC;
        proxyClass.name = String.format(PROXY_CLASS_FORMAT, vanillaClassName);
        proxyClass.superName = "java/lang/Object";
        proxyClass.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, PROXY_FIELD, vanillaClassDesc, null, null);
        MethodVisitor constructor = proxyClass.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + vanillaClassDesc + ")V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitVarInsn(Opcodes.ALOAD, 1);
        constructor.visitFieldInsn(Opcodes.PUTFIELD, proxyClass.name, PROXY_FIELD, vanillaClassDesc);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        return proxyClass;
    }

    private boolean splitInterfaces(ClassNode untransformed, ClassNode transformed, ClassNode proxyClass) {
        if (transformed.interfaces == null) {
            return false;
        }

        Set<String> addedInterfaces = new LinkedHashSet<>(transformed.interfaces);
        if (untransformed.interfaces != null) {
            untransformed.interfaces.forEach(addedInterfaces::remove);
        }
        if (!addedInterfaces.isEmpty()) {
            addInterfaces(untransformed, transformed, proxyClass, addedInterfaces);
            return true;
        }
        return false;
    }

    private boolean splitFields(ClassNode untransformed, ClassNode transformed, ClassNode proxyClass) {
        if (transformed.fields == null) {
            return false;
        }

        Map<String, FieldNode> addedFields = new LinkedHashMap<>(transformed.fields.size());
        for (FieldNode field : transformed.fields) {
            addedFields.put(field.name + ":" + field.desc, field);
        }
        if (untransformed.fields != null) {
            untransformed.fields.forEach(field -> addedFields.remove(field.name + ":" + field.desc));
        }
        Iterator<FieldNode> fieldItr = transformed.fields.iterator();
        while (fieldItr.hasNext()) {
            FieldNode field = fieldItr.next();
            if (addedFields.containsKey(field.name + ":" + field.desc)) {
                fieldItr.remove();
                field.access &= ~Opcodes.ACC_FINAL; // don't require field to be initialized in constructor
                if (proxyClass.fields == null) {
                    proxyClass.fields = new ArrayList<>();
                }
                proxyClass.fields.add(field);
            }
        }
        return !addedFields.isEmpty();
    }

    private boolean splitMethods(ClassNode untransformed, ClassNode transformed, ClassNode proxyClass) {
        if (transformed.methods == null) {
            return false;
        }

        Map<String, MethodNode> addedMethods = new LinkedHashMap<>(transformed.methods.size());
        for (MethodNode method : transformed.methods) {
            addedMethods.put(method.name + method.desc, method);
        }
        if (untransformed.methods != null) {
            untransformed.methods.forEach(method -> addedMethods.remove(method.name + method.desc));
        }
        Iterator<MethodNode> methodItr = transformed.methods.iterator();
        while (methodItr.hasNext()) {
            MethodNode method = methodItr.next();
            if (addedMethods.containsKey(method.name + method.desc)) {
                methodItr.remove();
                if (proxyClass.methods == null) {
                    proxyClass.methods = new ArrayList<>();
                }
                proxyClass.methods.add(method);
            }
        }
        return !addedMethods.isEmpty();
    }

    private void addInterfaces(ClassNode untransformed, ClassNode transformed, ClassNode proxyClass, Set<String> interfaces) {
        // make sure transformed has the exact same interfaces as untransformed
        transformed.interfaces.clear();
        if (untransformed.interfaces != null) {
            transformed.interfaces.addAll(untransformed.interfaces);
        }

        proxyClass.interfaces = new ArrayList<>(interfaces);
        for (String itf : interfaces) {
            classesImplementingInterface.computeIfAbsent(itf, k -> ConcurrentHashMap.newKeySet()).add(proxyClass.name);
        }
        Set<String> classesToRetransform = new HashSet<>();
        for (String itf : interfaces) {
            classesToRetransform.addAll(classesUsingInterface.getOrDefault(itf, Collections.emptySet()));
        }
        fixerContext.requestRetransform(classesToRetransform);
    }

    private void fixupMoves(ClassNode accessingClass, ClassNode accessedClass, Supplier<AbstractInsnNode> convertInsn) {
        if (accessingClass.methods == null) {
            return;
        }

        for (MethodNode method : accessingClass.methods) {
            Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
            Frame<SourceValue>[] frames = analyzer.analyzeAndComputeMaxs(accessingClass.name, method);
        }
    }
}
