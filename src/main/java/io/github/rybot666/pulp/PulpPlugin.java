package io.github.rybot666.pulp;

import io.github.rybot666.pulp.util.log.PulpLogger;
import io.github.rybot666.pulp.util.UnsafeUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PulpPlugin extends JavaPlugin {
    public static final Logger LOGGER = new PulpLogger(PulpPlugin.class, PulpBootstrap.NAME);

    private static boolean hasInitialised = false;

    private static void addSelfToMinecraftClassPath() {
        try {
            URL pluginJarLocation = PulpPlugin.class.getProtectionDomain().getCodeSource().getLocation();

            if (!(Bukkit.class.getClassLoader() instanceof URLClassLoader)) {
                // we're in Java 9+ without the server bundler existing, we're on the system class loader
                PulpBootstrap.INSTRUMENTATION.appendToSystemClassLoaderSearch(new JarFile(pluginJarLocation.getFile()));
                return;
            }

            UnsafeUtils.addToURLClassPath((URLClassLoader) Bukkit.class.getClassLoader(), pluginJarLocation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load own jar file", e);
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
            LOGGER.log(Level.SEVERE, "Error occurred during bootstrap! Stopping...", e);

            Bukkit.shutdown();
            return;
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
