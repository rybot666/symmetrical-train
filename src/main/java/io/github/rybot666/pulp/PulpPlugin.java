package io.github.rybot666.pulp;

import io.github.rybot666.pulp.util.PulpLogger;
import io.github.rybot666.pulp.util.UnsafeUtil;
import io.github.rybot666.pulp.util.Util;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class PulpPlugin extends JavaPlugin {
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION = ByteBuddyAgent.install();
    public static final Logger LOGGER = new PulpLogger(PulpPlugin.class, NAME);

    private static boolean hasInitialised = false;

    static {
        addSelfToMinecraftClassPath();

        try {
            Class.forName("io.github.rybot666.pulp.MinecraftClassLoaded", true, Bukkit.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Reflection error", e);
        }
    }

    private static void addSelfToMinecraftClassPath() {
        try {
            URL pluginJarLocation = PulpPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL();

            if (!(Bukkit.class.getClassLoader() instanceof URLClassLoader)) {
                // we're in Java 9+ without the server bundler existing, we're on the system class loader
                INSTRUMENTATION.appendToSystemClassLoaderSearch(new JarFile(pluginJarLocation.getFile()));
                return;
            }

            UnsafeUtil.addToURLClassPath((URLClassLoader) Bukkit.class.getClassLoader(), pluginJarLocation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load own jar file", e);
        } catch (URISyntaxException e) {
            throw new AssertionError("Something has gone horribly wrong", e);
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
