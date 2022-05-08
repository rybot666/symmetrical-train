package io.github.rybot666.pulp;

import io.github.rybot666.pulp.discovery.PluginStateManager;
import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import io.github.rybot666.pulp.util.log.LogUtils;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.service.MixinService;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class PulpBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger("Bootstrap");
    
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION;
    public static boolean DEBUG = System.getProperty("pulp.debug", "false").equals("true");

    static {
        try {
            INSTRUMENTATION = ByteBuddyAgent.install();

            if (!INSTRUMENTATION.isRetransformClassesSupported()) {
                throw new RuntimeException("Your JVM does not support class retransformation! Pulp will not work");
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException("Pulp has failed to install an agent, this is required!", e);
        }
    }

    @SuppressWarnings("unused") // Used reflectively
    public static void init(Plugin plugin) {
        if (DEBUG) {
            LOGGER.info("Running in debug mode");
        }

        MixinBootstrap.init();
        MixinBootstrap.getPlatform().inject();

        // Perform mixin discovery
        PluginStateManager.init(plugin);

        // Perform retransformation for existing mixins
        PulpMixinService service = (PulpMixinService) MixinService.getService();
        service.pulpTransformer.retransformAllLoadedClasses();
    }
}
