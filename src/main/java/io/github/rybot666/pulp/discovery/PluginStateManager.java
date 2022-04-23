package io.github.rybot666.pulp.discovery;

import com.google.gson.Gson;
import io.github.rybot666.pulp.PulpPlugin;
import io.github.rybot666.pulp.listener.BaseListener;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import io.github.rybot666.pulp.util.reflect.FieldUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixins;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginStateManager implements Listener {
    private static final PulpLogger LOGGER = LogUtils.getLogger();
    private static final List<Plugin> PLUGINS = new ArrayList<>();
    private static final StateListener LISTENER = new StateListener();

    public static final String CONFIG_LOCATION = "pulp.config.json";

    static {
        for (Plugin plugin : getAllLoadedPlugins()) {
            add(plugin);
        }
    }

    private PluginStateManager() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    @SuppressWarnings("unchecked")
    private static List<Plugin> getAllLoadedPlugins() {
        SimplePluginManager pluginManager = (SimplePluginManager) Bukkit.getPluginManager();

        try {
            Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);

            return (List<Plugin>) pluginsField.get(pluginManager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to reflect plugins out of SimplePluginManager", e);
        }
    }

    public static void init(Plugin owner) {
        LISTENER.register(owner);
    }

    private static void add(Plugin plugin) {
        PLUGINS.add(plugin);

        // Perform mixin discovery
        File pluginFile = (File) FieldUtils.get(plugin, JavaPlugin.class, "file");
        PluginDescriptionFile descriptionFile = plugin.getDescription();

        LOGGER.info(() -> String.format("Discovered %s, searching for mixin configs", descriptionFile.getName()));

        if (!descriptionFile.getDepend().contains(PulpPlugin.NAME)) {
            // Plugins that don't depend on us are not trying to load mixins
            return;
        }

        try {
            JarFile jarFile = new JarFile(pluginFile);
            JarEntry entry = jarFile.getJarEntry(CONFIG_LOCATION);

            if (entry == null) {
                LOGGER.warning(() -> String.format("%s depends on Pulp, but does not have a %s!", descriptionFile.getName(), CONFIG_LOCATION));
                return;
            }

            try (
                    InputStream is = jarFile.getInputStream(entry);
                    InputStreamReader streamReader = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(streamReader)
            ) {
                PulpConfigFile config = new Gson().fromJson(reader, PulpConfigFile.class);

                LOGGER.info(() -> String.format("Parsed %s mixin config(s) from %s", config.mixins.size(), descriptionFile.getName()));
                config.mixins.forEach(Mixins::addConfiguration);
            }
        } catch (IOException e) {
            throw new RuntimeException("IO Exception while parsing JAR", e);
        }
    }

    private static class StateListener extends BaseListener {
        @EventHandler
        private void onPluginEnabled(PluginEnableEvent event) {
            Plugin plugin = event.getPlugin();

            if (plugin.getName().equals(PulpPlugin.NAME)) return;

            if (PLUGINS.contains(plugin)) {
                LOGGER.warning(() -> String.format("%s has been enabled twice! This may cause problems", plugin.getName()));
                return;
            }

            add(plugin);
        }

        @EventHandler
        private void onPluginDisabled(PluginDisableEvent event) {
            Plugin plugin = event.getPlugin();

            if (PLUGINS.contains(plugin)) {
                LOGGER.warning(() -> String.format("%s has been disabled! This may cause problems", plugin.getName()));
            }
        }
    }
}
