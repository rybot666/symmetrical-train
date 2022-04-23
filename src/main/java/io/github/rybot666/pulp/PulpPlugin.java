package io.github.rybot666.pulp;

import io.github.rybot666.pulp.util.log.PulpLogger;
import io.github.rybot666.pulp.util.UnsafeUtil;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        // TODO: reconsider this
        if (PulpPlugin.hasInitialised) {
            throw new UnsupportedOperationException("Cannot reload when mixins are in use - this command is deprecated");
        }

        addSelfToMinecraftClassPath();

        try {
            Class<?> clazz = Class.forName("io.github.rybot666.pulp.PulpBootstrap", true, Bukkit.class.getClassLoader());
            Method initMethod = clazz.getDeclaredMethod("init", Plugin.class);

            initMethod.invoke(null, this);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error occurred during Pulp Bootstrap", e);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reflection error while loading onto MC classpath", e);
        }

        PulpPlugin.hasInitialised = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
