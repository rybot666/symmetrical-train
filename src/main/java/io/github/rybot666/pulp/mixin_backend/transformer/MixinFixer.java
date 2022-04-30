package io.github.rybot666.pulp.mixin_backend.transformer;

import io.github.rybot666.pulp.PulpBootstrap;
import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import io.github.rybot666.pulp.mixin_backend.transformer.transformations.TransformationState;
import io.github.rybot666.pulp.util.Lazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MixinFixer {
    public final Lazy<InterfaceCache> interfaceCache;
    public final Map<String, TransformationState> transformationQueue = new ConcurrentHashMap<>();

    public MixinFixer(PulpMixinService owner) {
        this.interfaceCache = new Lazy<>(() -> {
            InterfaceCache cache = new InterfaceCache(owner);
            cache.registerAllClasses(PulpBootstrap.INSTRUMENTATION);

            return cache;
        });
    }
}
