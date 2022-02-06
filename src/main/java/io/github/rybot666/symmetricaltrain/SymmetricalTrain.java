package io.github.rybot666.symmetricaltrain;

import io.github.rybot666.symmetricaltrain.mixinbackend.MixinService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SymmetricalTrain extends JavaPlugin {
    private static boolean hasInitialised = false;

    private MixinService mixinService;

    @Override
    public void onEnable() {
        // using /reload will break everything
        if (SymmetricalTrain.hasInitialised) {
            throw new UnsupportedOperationException("Cannot reload when mixins are in use");
        }

        SymmetricalTrain.hasInitialised = true;

        this.mixinService = new MixinService(this);
    }
}
