package io.github.rybot666.pulp;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.instrument.Instrumentation;

public final class PulpPlugin extends JavaPlugin {
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION = ByteBuddyAgent.install();

    private static boolean hasInitialised = false;

    @Override
    public void onEnable() {
        // using /reload will break everything
        if (PulpPlugin.hasInitialised) {
            throw new UnsupportedOperationException("Cannot reload when mixins are in use - this command is deprecated");
        }

        PulpPlugin.hasInitialised = true;
    }
}
