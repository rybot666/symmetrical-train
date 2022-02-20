package io.github.rybot666.pulp.mixinservice;

import io.github.rybot666.pulp.PulpPlugin;
import io.github.rybot666.pulp.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.logging.Level;

public class PulpTransformer implements ClassFileTransformer {
    private final PulpMixinService owner;

    public PulpTransformer(PulpMixinService owner) {
        this.owner = owner;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        ClassNode untransformed = Util.readNode(classfileBuffer);
        ClassNode transformed = new ClassNode();
        untransformed.accept(transformed);

        this.owner.fixer.registerClass(untransformed);

        if (!this.owner.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), className, transformed)) {
            return null;
        }

        this.owner.fixer.splitClass(untransformed, transformed);

        return Util.writeNode(transformed);
    }

    public static PulpTransformer register(Instrumentation instrumentation, PulpMixinService owner) {
        PulpTransformer transformer = new PulpTransformer(owner);
        instrumentation.addTransformer(transformer, false);

        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (int i = 0; i < allLoadedClasses.length; i++) {
            Class<?> clazz = allLoadedClasses[i];
            int indexToDisplay = i + 1;
            PulpPlugin.LOGGER.log(Level.FINEST, () -> String.format("Processing loaded class %d/%d : %s", indexToDisplay, allLoadedClasses.length, clazz.getName()));
            if (clazz.isArray() || clazz.isPrimitive()) {
                continue;
            }
            try {
                ClassReader reader = Util.getClassReader(clazz.getClassLoader(), clazz.getName());
                if (reader == null) {
                    continue;
                }
                ClassNode node = Util.readNode(reader);

                owner.fixer.registerClass(node);
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
