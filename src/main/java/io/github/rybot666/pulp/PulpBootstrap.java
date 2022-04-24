package io.github.rybot666.pulp;

import io.github.rybot666.pulp.discovery.PluginStateManager;
import io.github.rybot666.pulp.mixin_backend.service.PulpMixinService;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.plugin.Plugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.service.MixinService;

import java.lang.instrument.Instrumentation;
import java.util.Random;

public class PulpBootstrap {
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION;

    static {
        try {
            INSTRUMENTATION = ByteBuddyAgent.install();

            if (!INSTRUMENTATION.isRetransformClassesSupported()) {
                throw new RuntimeException("Your JVM does not support class retransformation! Pulp will not work");
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException("Pulp has failed to install an agent, this is required", e);
        }
    }

    @SuppressWarnings("unused") // Used reflectively
    public static void init(Plugin plugin) {
        MixinBootstrap.init();
        MixinBootstrap.getPlatform().inject();

        // Perform mixin discovery
        PluginStateManager.init(plugin);

        // Perform retransformation for existing mixins
        PulpMixinService service = (PulpMixinService) MixinService.getService();
        service.pulpTransformer.retransformAllLoadedClasses();

        Random random = new Random();
        System.out.println(random.nextInt());
        System.out.println(random.nextInt());
        System.out.println(random.nextInt());
    }
}
