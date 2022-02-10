package io.github.rybot666.pulp;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class PulpPlugin extends JavaPlugin {
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION = ByteBuddyAgent.install();
    public static final Logger LOGGER = Logger.getLogger(NAME);

    private static boolean hasInitialised = false;

    static {
        try {
            JarFile jar = new JarFile(new File(PulpPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            INSTRUMENTATION.appendToSystemClassLoaderSearch(jar);

            Class.forName("io.github.rybot666.pulp.SystemClassLoaded", true, ClassLoader.getSystemClassLoader());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load own jar file", e);
        } catch (URISyntaxException e) {
            throw new AssertionError("Something has gone horribly wrong", e);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing Mixin");
        }
    }

    @Override
    public void onEnable() {
        // using /reload will break everything
        if (PulpPlugin.hasInitialised) {
            throw new UnsupportedOperationException("Cannot reload when mixins are in use - this command is deprecated");
        }

        PulpPlugin.hasInitialised = true;
    }
}
