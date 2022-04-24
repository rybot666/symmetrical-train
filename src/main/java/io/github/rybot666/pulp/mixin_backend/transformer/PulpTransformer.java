package io.github.rybot666.pulp.mixin_backend.transformer;

import io.github.rybot666.pulp.PulpBootstrap;
import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import io.github.rybot666.pulp.mixin_backend.transformer.fixer.ClassDiff;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PulpTransformer implements ClassFileTransformer {
    private static final PulpLogger LOGGER = LogUtils.getLogger("Transformer");
    private final PulpMixinService owner;

    public PulpTransformer(PulpMixinService owner) {
        this.owner = owner;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        LOGGER.finest(() -> String.format("Class transformation requested for %s", className));

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode untransformed = new ClassNode();

        reader.accept(untransformed, ClassReader.EXPAND_FRAMES);

        ClassNode transformed = new ClassNode();
        untransformed.accept(transformed);

        this.owner.fixer.interfaceCache.processClass(untransformed);

        if (!this.owner.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), className.replace('/', '.'), transformed)) {
            return null;
        }

        ClassDiff diff = new ClassDiff(untransformed, transformed);

        ClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        transformed.accept(writer);

        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private List<IMixinConfig> getMixinConfigs() {
        // This has to be done via reflection, gross
        try {
            Field processorField = this.owner.transformer.getClass().getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(this.owner.transformer);

            Field configField = processor.getClass().getDeclaredField("configs");
            configField.setAccessible(true);
            return (List<IMixinConfig>) configField.get(processor);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Couldn't reflect configs out of MixinTransformer processor", e);
        }
    }

    /**
     * Performs mixin application to all loaded classes
     */
    public void retransformAllLoadedClasses() {
        List<Class<?>> transformableClasses = new ArrayList<>();

        for (Class<?> clazz: PulpBootstrap.INSTRUMENTATION.getAllLoadedClasses()) {
            if (clazz.isSynthetic() || clazz.isPrimitive()) continue;

            transformableClasses.add(clazz);
        }

        LOGGER.info(() -> String.format("Searching for mixins to apply (checking %d classes)", transformableClasses.size()));

        // First pass, locate all classes that need a retransform
        Set<Class<?>> retransformTargets = new HashSet<>();
        List<IMixinConfig> configs = this.getMixinConfigs();
        configs.get(0).getTargets();

        Set<String> allTargets = this.getMixinConfigs().stream()
                .map(IMixinConfig::getTargets)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        for (Class<?> clazz: transformableClasses) {
            if (allTargets.contains(clazz.getName())) {
                LOGGER.fine(() -> String.format("Found mixins for %s", clazz.getName()));
                retransformTargets.add(clazz);
            }
        }

        // Attempt retransformation of all mixin targets
        LOGGER.info(() -> String.format("Found %d mixin target classes to retransform", retransformTargets.size()));


        for (Class<?> clazz: retransformTargets) {
            try {
                PulpBootstrap.INSTRUMENTATION.retransformClasses(clazz);
            } catch (UnmodifiableClassException e) {
                LOGGER.warning(() -> String.format("Retransformation failure! %s is an unmodifiable class", clazz.getName()));
            } catch (UnsupportedOperationException e) {
                LOGGER.severe("********************");
                LOGGER.severe("Unsupported operation occurred during a mixin class retransformation!");
                LOGGER.severe("This means that Pulp has missed an illegal class transformation - this is a bug. Please report it so we can fix it");
                LOGGER.severe("");
                LOGGER.severe(() -> String.format("Target class: %s. Reason: %s (full trace below)", clazz.getName(), e));
                LOGGER.severe("********************");
                LOGGER.log(Level.SEVERE, "", e);
            }
        }

        LOGGER.info("Mixin retransformations complete");

        // TODO(rybot666): retransformation of classes that access this is needed in some cases. Will be done as part of the diff process
    }

    public static PulpTransformer register(Instrumentation instrumentation, PulpMixinService owner) {
        PulpTransformer transformer = new PulpTransformer(owner);
        instrumentation.addTransformer(transformer, true);

        return transformer;
    }
}
