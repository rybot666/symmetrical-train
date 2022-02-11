package io.github.rybot666.pulp.instrumentation;

import io.github.rybot666.pulp.PulpPlugin;
import io.github.rybot666.pulp.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Transformer to watch for classes being loaded or redefined
 */
public class ClassLoadWatchTransformer implements ClassFileTransformer {
    private final Consumer<ClassNode> consumer;

    public ClassLoadWatchTransformer(Consumer<ClassNode> consumer) {
        this.consumer = consumer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode node = Util.readNode(reader);

        this.consumer.accept(node);

        return null;
    }

    public static ClassLoadWatchTransformer register(Instrumentation instrumentation, Consumer<ClassNode> consumer) {
        ClassLoadWatchTransformer transformer = new ClassLoadWatchTransformer(consumer);
        instrumentation.addTransformer(transformer, false);

        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (int i = 0; i < allLoadedClasses.length; i++) {
            PulpPlugin.LOGGER.log(Level.INFO, String.format("Processing loaded class %d/%d : %s", i + 1, allLoadedClasses.length, allLoadedClasses[i].getName()));
            Class<?> clazz = allLoadedClasses[i];
            if (clazz.isArray() || clazz.isPrimitive()) {
                continue;
            }
            try {
                ClassReader reader = Util.getClassReader(clazz.getClassLoader(), clazz.getName());
                if (reader == null) {
                    continue;
                }
                ClassNode node = Util.readNode(reader);

                consumer.accept(node);
            } catch (ClassNotFoundException e) {
                PulpPlugin.LOGGER.log(Level.WARNING, String.format("Missing expected class \"%s\"", clazz.getName()),
                        e);
            } catch (IOException e) {
                PulpPlugin.LOGGER.log(Level.WARNING, String.format("Failed to read class data for class \"%s\"",
                        clazz.getName()), e);
            }
        }

        return transformer;
    }
}
