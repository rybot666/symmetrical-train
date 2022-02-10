package io.github.rybot666.pulp;

import org.spongepowered.asm.launch.MixinBootstrap;

public class SystemClassLoaded {
    static {
        MixinBootstrap.init();
    }
}
