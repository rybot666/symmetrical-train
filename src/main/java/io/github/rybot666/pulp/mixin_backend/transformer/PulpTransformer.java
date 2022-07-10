package io.github.rybot666.pulp.mixin_backend.transformer;

import io.github.rybot666.pulp.PulpBootstrap;
import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import io.github.rybot666.pulp.mixin_backend.transformer.instrumentation_compat.FieldDefinalizationHandler;
import io.github.rybot666.pulp.mixin_backend.transformer.instrumentation_compat.MethodAdditionHandler;
import io.github.rybot666.pulp.mixin_backend.transformer.proxy.ProxyFactory;
import io.github.rybot666.pulp.mixin_backend.transformer.proxy.ProxyMarker;
import io.github.rybot666.pulp.mixin_backend.transformer.remap.FieldDefinalizationRemapper;
import io.github.rybot666.pulp.mixin_backend.transformer.remap.MassClassState;
import io.github.rybot666.pulp.proxies.GlobalProxyState;
import io.github.rybot666.pulp.util.DebugUtils;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.bukkit.Bukkit;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

// TODO: implement transformers for mixin operations that Instrumentation cannot directly do
// - Some injects generate handler methods which are then called
//   > Add these to proxy method, retransform usages in class (no global search required)
//   > (opt) This can be inlined which reduces the requirement for proxy classes in some cases
// - `@Mutable` definalises fields
//   > Generate a proxy class with a definalised field (same name etc)
//   > Replace all uses globally with this proxy field
//   > (opt) Private fields only need to be changed in the target
//   > (feat) Somehow let reflection work on these?
// - Interfaces added (via `@Interface` or `implements` on the mixin)
//   > Add the interface to the proxy class
//   > Transform all usages of the class in interface position to use the proxy class
// - (feat) Access wideners? (this is hard, not part of mixin, would be nice to have tho)
// - Mixin inheritance causes problems if you aren't careful

/**
 * The entrypoint of the Pulp transformer chain
 */
public class PulpTransformer implements ClassFileTransformer {
    private static final PulpLogger LOGGER = LogUtils.getLogger("Transformer");

    private final PulpMixinService owner;

    public final MassClassState massClassState = new MassClassState();
    public final FieldDefinalizationRemapper definalizationRemapper = new FieldDefinalizationRemapper();
    public final RetransformQueue retransformQueue = new RetransformQueue();

    public PulpTransformer(PulpMixinService owner) {
        this.owner = owner;
    }

    private static void possiblyDumpClass(String kind, ClassNode node) {
        possiblyDumpClass(kind, node.name, node);
    }

    private static void possiblyDumpClass(String kind, String name, ClassNode node) {
        if (PulpBootstrap.DEBUG) {
            Path classPath = Paths.get(".pulp.out", "bytecode", kind, name.concat(".class"));
            Path tracePath = Paths.get(".pulp.out", "trace", kind, name.concat(".trace.txt"));

            try {
                DebugUtils.dumpClass(classPath, node);
                DebugUtils.checkAndDumpTrace(tracePath, node);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, String.format("IO Exception while dumping class bytecode (type %s)", kind), e);
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            LOGGER.finest(() -> String.format("Class transformation requested for %s", className));

            // Parse a classnode from the passed bytes
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode untransformed = new ClassNode();

            reader.accept(untransformed, ClassReader.EXPAND_FRAMES);

            ClassNode transformed = new ClassNode();
            untransformed.accept(transformed);

            // Parse all global state from the class
            this.massClassState.process(untransformed);

            /*
             * 1) Pass the class into mixin
             *    - This applies any mixins registered against the class
             */
            possiblyDumpClass("untransformed", untransformed);
            this.owner.transformer.transformClass(MixinEnvironment.getCurrentEnvironment(), className.replace('/', '.'), transformed);

            possiblyDumpClass("post_mixin", transformed);

            /*
             * 2) Transform output class for Instrumentation compatibility
             *    - This takes the class bytecode that mixin generates and outputs a class that can be transformed with instrumentation
             *    - This step may also generate a "proxy" class that stores changes that cannot be applied via instrumentation
             *    - If any incompatible changes are made, they may register for a global remap
             *
             * 3) Perform class remapping
             *    - If any global changes are registered against this class, they will be applied now
             */
            boolean shouldLoadProxy;

            // Handle field definalization
            ClassNode proxy = ProxyFactory.generateBaseProxy(Type.getObjectType(transformed.name));
            shouldLoadProxy = FieldDefinalizationHandler.handle(this, untransformed, transformed, proxy);

            // Then possibly remap the proxy and transformed classes to ensure changes apply properly
            this.definalizationRemapper.remap(transformed);
            shouldLoadProxy |= this.definalizationRemapper.remap(proxy);

            // And then move over methods to proxy
            shouldLoadProxy |= MethodAdditionHandler.handle(untransformed, transformed, proxy);

            possiblyDumpClass("transformed", transformed);

            // Write and define proxy class
            try {
                if (shouldLoadProxy) {
                    possiblyDumpClass("proxy", DebugUtils.getOriginalClassName(proxy.name), proxy);

                    MixinClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    proxy.accept(writer);

                    // Classes are loaded lazily because classloaders
                    byte[] proxyBytes = writer.toByteArray();
                    synchronized (GlobalProxyState.WAITING_PROXY_CLASSES) {
                        GlobalProxyState.WAITING_PROXY_CLASSES.put(transformed.name, proxyBytes);
                    }
                }
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, e, () -> String.format("Error while defining proxy class for %s", transformed.name));
                Bukkit.shutdown();
            }

            // Write back class bytes
            ClassWriter writer = new MixinClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            transformed.accept(writer);

            // Check if the current retransform queue is empty and the next one is not
            //
            // If this is the case then we should pop the queue and retransform all targets in the "current" list
            if (this.retransformQueue.isCurrentEmpty() && !this.retransformQueue.isNextEmpty()) {
                Set<Class<?>> targets = this.retransformQueue.shift();
                targets.forEach(this::retransformClass);
            }

            return writer.toByteArray();
        } catch (Throwable th) {
            LOGGER.log(Level.SEVERE, th, () -> String.format("An error occurred during class transformation on %s", className));
            return null;
        }
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
     * Populates the retransform queue with all mixin-ed loaded classes, and collects class state from all of them
     */
    public void processAllLoadedClass() {
        // Find all transformable classes
        List<Class<?>> transformableClasses = new ArrayList<>();

        for (Class<?> clazz : PulpBootstrap.INSTRUMENTATION.getAllLoadedClasses()) {
            if (clazz.isSynthetic() || clazz.isPrimitive() || clazz.isArray() || !PulpBootstrap.INSTRUMENTATION.isModifiableClass(clazz)) continue;

            transformableClasses.add(clazz);
        }

        // Search for all mixins applied to the discovered targets
        LOGGER.info(() -> String.format("Searching for mixins to apply (checking %d classes)", transformableClasses.size()));
        Set<Class<?>> retransformTargets = new HashSet<>();

        Set<String> allTargets = this.getMixinConfigs().stream()
                .map(IMixinConfig::getTargets)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        for (Class<?> clazz : transformableClasses) {
            if (allTargets.contains(clazz.getName())) {
                LOGGER.fine(() -> String.format("Found mixins for %s", clazz.getName()));
                retransformTargets.add(clazz);
            }
        }

        // Push all targets into the retransform queue
        LOGGER.info(() -> String.format("Found %d mixin target classes to retransform", retransformTargets.size()));
        this.retransformQueue.pushAll(retransformTargets);
    }

    public void retransformAllInQueue() {
        // Retransform everything in the queue
        Set<Class<?>> targets;

        long start = System.currentTimeMillis();
        LOGGER.info("Retransforming all classes in queue...");

        targets = this.retransformQueue.shift();
        LOGGER.info(String.format("Retransforming %d initial classes from queue", targets.size()));

        targets.forEach(this::retransformClass);

        long diff = System.currentTimeMillis() - start;
        LOGGER.info(() -> String.format("Retransformation complete (took %d ms in %d stages)", diff, this.retransformQueue.getStage()));
    }

    /**
     * Retransforms the provided class
     */
    public void retransformClass(Class<?> clazz) {
        try {
            PulpBootstrap.INSTRUMENTATION.retransformClasses(clazz);
        } catch (UnmodifiableClassException e) {
            LOGGER.severe(() -> String.format("Retransformation failure! %s is an unmodifiable class", clazz.getName()));
        } catch (UnsupportedOperationException e) {
            LOGGER.severe("********************");
            LOGGER.severe("Unsupported operation occurred during mixin application!");
            LOGGER.severe("This means that Pulp has missed an illegal class retransformation - this is a bug. Please report it!");
            LOGGER.severe("");
            LOGGER.severe(() -> String.format("Target class: %s. Reason: %s (full trace below)", clazz.getName(), e));
            LOGGER.log(Level.SEVERE, "********************", e);
        }
    }

    public static PulpTransformer register(Instrumentation instrumentation, PulpMixinService owner) {
        PulpTransformer transformer = new PulpTransformer(owner);
        instrumentation.addTransformer(transformer, true);

        return transformer;
    }
}
