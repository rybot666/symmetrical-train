package io.github.rybot666.pulp.mixin_backend.fixer;

import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import org.objectweb.asm.tree.ClassNode;

public class MixinFixer {
    public final InterfaceCache interfaceCache;

    public MixinFixer(PulpMixinService owner) {
        this.interfaceCache = new InterfaceCache(owner);
    }

    public void splitClass(ClassNode untransformed, ClassNode transformed) {
    }
}
