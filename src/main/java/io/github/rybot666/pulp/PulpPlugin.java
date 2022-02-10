package io.github.rybot666.pulp;

import io.github.rybot666.pulp.util.Util;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class PulpPlugin extends JavaPlugin {
    public static final String NAME = "Pulp";
    public static final Instrumentation INSTRUMENTATION = ByteBuddyAgent.install();
    public static final Logger LOGGER = Logger.getLogger(NAME);

    private static boolean hasInitialised = false;

    static {
        addSelfToMinecraftClassPath();

        try {
            Class.forName("io.github.rybot666.pulp.MinecraftClassLoaded", true, Bukkit.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Reflection error", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addSelfToMinecraftClassPath() {
        try {
            URL pluginJarLocation = PulpPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI().toURL();

            if (!(Bukkit.class.getClassLoader() instanceof URLClassLoader)) {
                // we're in Java 9+ without the server bundler existing, we're on the system class loader
                INSTRUMENTATION.appendToSystemClassLoaderSearch(new JarFile(pluginJarLocation.getFile()));
                return;
            }

            // emulate URLClassLoader.addURL
            Object ucp = Util.getFieldWithUnsafe(Bukkit.class.getClassLoader(), URLClassLoader.class, "ucp");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (ucp) {
                if (Util.getBooleanWithUnsafe(ucp, ucp.getClass(), "closed")) {
                    throw new AssertionError("UCP is closed");
                }
                Collection<URL> urls = (Collection<URL>) Util.getFieldWithUnsafe(ucp, ucp.getClass(), "unopenedUrls");
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (urls) {
                    Collection<URL> path = (Collection<URL>) Util.getFieldWithUnsafe(ucp, ucp.getClass(), "path");
                    if (!path.contains(pluginJarLocation)) {
                        urls.add(pluginJarLocation);
                        path.add(pluginJarLocation);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load own jar file", e);
        } catch (URISyntaxException e) {
            throw new AssertionError("Something has gone horribly wrong", e);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Reflection error", e);
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
