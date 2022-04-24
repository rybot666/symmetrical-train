package io.github.rybot666.pulp.mixin_backend.transformer.fixer;

import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import io.github.rybot666.pulp.util.Util;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.asm.ASM;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class InterfaceCache {
    private static final PulpLogger LOGGER = LogUtils.getLogger();

    private final Map<String, Set<String>> classesImplementingInterface = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> classesUsingInterface = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isInterfaceCache = new ConcurrentHashMap<>();
    private final Set<String> processedClasses = ConcurrentHashMap.newKeySet();
    private final PulpMixinService service;

    public InterfaceCache(PulpMixinService service) {
        this.service = service;
    }

    private boolean isInterface(Type type) {
        String internalName = type.getInternalName();
        if (this.isInterfaceCache.containsKey(internalName)) {
            return this.isInterfaceCache.get(internalName);
        }

        boolean isInterface = false;

        try {
            ClassReader reader = Util.getClassReader(this.service.hackyClassLoader, internalName);
            isInterface = reader != null && (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e, () -> String.format("IO Exception while reading class %s", internalName));
        }

        this.isInterfaceCache.put(internalName, isInterface);
        return isInterface;
    }

    private void registerUsage(ClassNode node, Type using) {
        if (using.getSort() == Type.OBJECT) {
            if (this.isInterface(using)) {
                this.classesUsingInterface.computeIfAbsent(using.getInternalName(), k -> ConcurrentHashMap.newKeySet()).add(node.name);
            }
        } else if (using.getSort() == Type.ARRAY) {
            Type elementType = using.getElementType();
            registerUsage(node, elementType);
        }
    }

    private void registerMethodArgsAndReturn(ClassNode node, String descriptor) {
        Type methodType = Type.getMethodType(descriptor);

        for (Type type: methodType.getArgumentTypes()) {
            this.registerUsage(node, type);
        }

        this.registerUsage(node, methodType.getReturnType());
    }

    public void processClass(ClassNode node) {
        // Ignore synthetic classes
        if ((node.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return;
        }

        if (this.processedClasses.contains(node.name)) return;

        if (node.interfaces != null) {
            for (String itf: node.interfaces) {
                this.classesImplementingInterface.computeIfAbsent(itf, k -> ConcurrentHashMap.newKeySet()).add(node.name);
            }
        }

        if (node.methods != null) {
            for (MethodNode method: node.methods) {
                this.registerMethodArgsAndReturn(node, method.desc);

                method.accept(new MethodVisitor(ASM.API_VERSION) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        registerUsage(node, Type.getObjectType(type));
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        registerUsage(node, Type.getObjectType(owner));
                        registerUsage(node, Type.getType(descriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        registerUsage(node, Type.getObjectType(owner));
                        registerMethodArgsAndReturn(node, descriptor);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        registerMethodArgsAndReturn(node, descriptor);
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        registerUsage(node, Type.getType(descriptor));
                    }
                });
            }
        }

        this.processedClasses.add(node.name);
    }

    public void registerAllClasses(Instrumentation instrumentation) {
        long start = System.currentTimeMillis();

        Class<?>[] allLoaded = instrumentation.getAllLoadedClasses();
        LOGGER.info(() -> String.format("Searching %d classes for interfaces...", allLoaded.length));

        int invalidCount = 0;

        for (Class<?> clazz : allLoaded) {
            try {
                if (clazz.isSynthetic() || clazz.isPrimitive()) {
                    continue;
                }

                if (clazz.isArray()) {
                    clazz = clazz.getComponentType();
                }

                ClassReader reader = Util.getClassReader(this.service.hackyClassLoader, clazz.getName());

                if (reader == null) {
                    invalidCount++;
                    continue;
                }

                ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_DEBUG);

                processClass(node);
            } catch (ClassNotFoundException ignored) {

            } catch (IOException e) {
                Class<?> finalClazz = clazz;
                LOGGER.log(Level.WARNING, e, () -> String.format("IO Exception while reading class %s", finalClazz.getName()));
            }
        }

        long diff = System.currentTimeMillis() - start;

        int finalInvalidCount = invalidCount;
        LOGGER.info(() -> String.format("Searched %d classes (%d missing bytecode) in %dms - found %d implementations and %d usages",
                allLoaded.length - finalInvalidCount, finalInvalidCount, diff, this.classesImplementingInterface.size(), this.classesUsingInterface.size()));
    }

    public Set<String> getClassesUsing(String interfaceName) {
        return this.classesUsingInterface.computeIfAbsent(interfaceName, k -> ConcurrentHashMap.newKeySet());
    }

    public Set<String> getClassesImplementing(String interfaceName) {
        return this.classesImplementingInterface.computeIfAbsent(interfaceName, k -> ConcurrentHashMap.newKeySet());
    }
}
