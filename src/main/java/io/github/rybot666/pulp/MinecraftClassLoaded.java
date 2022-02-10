package io.github.rybot666.pulp;

import org.spongepowered.asm.launch.MixinBootstrap;

public class MinecraftClassLoaded {
    static {
        MixinBootstrap.init();
    }
}
