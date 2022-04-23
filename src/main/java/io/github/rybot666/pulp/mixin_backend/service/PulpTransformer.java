package io.github.rybot666.pulp.mixin_backend.service;

import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PulpTransformer implements ClassFileTransformer {
    private static final PulpLogger LOGGER = LogUtils.getLogger("Transformer");
    private final PulpMixinService owner;

    public PulpTransformer(PulpMixinService owner) {
        this.owner = owner;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode untransformed = new ClassNode();

        reader.accept(untransformed, ClassReader.EXPAND_FRAMES);

        ClassNode transformed = new ClassNode();
        untransformed.accept(transformed);

        this.owner.fixer.interfaceCache.processClass(untransformed);

        if (!this.owner.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), className, transformed)) {
            return null;
        }

        this.owner.fixer.splitClass(untransformed, transformed);

        ClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        transformed.accept(writer);

        return writer.toByteArray();
    }

    public static PulpTransformer register(Instrumentation instrumentation, PulpMixinService owner) {
        PulpTransformer transformer = new PulpTransformer(owner);
        instrumentation.addTransformer(transformer, false);

        return transformer;
    }
}
