package io.github.rybot666.pulp;

import io.github.rybot666.pulp.discovery.PluginStateManager;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.launch.MixinBootstrap;

public class PulpBootstrap {
    @SuppressWarnings("unused") // Used reflectively
    public static void init(Plugin plugin) {
        MixinBootstrap.init();
        PluginStateManager.init(plugin);
    }
}
