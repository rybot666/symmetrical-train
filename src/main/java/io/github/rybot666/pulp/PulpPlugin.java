package io.github.rybot666.pulp;

import io.github.rybot666.pulp.mixinservice.PulpMixinService;
import org.bukkit.plugin.java.JavaPlugin;

public final class PulpPlugin extends JavaPlugin {
    public static final String NAME = "Pulp";

    private static boolean hasInitialised = false;

    private PulpMixinService mixinService;

    @Override
    public void onEnable() {
        // using /reload will break everything
        if (PulpPlugin.hasInitialised) {
            throw new UnsupportedOperationException("Cannot reload when mixins are in use - this command is deprecated");
        }

        PulpPlugin.hasInitialised = true;

        this.mixinService = new PulpMixinService(this);
    }
}
