package io.github.rybot666.pulp.listener;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class BaseListener implements Listener {
    private boolean registered;

    public void register(Plugin plugin) {
        if (this.registered) {
            throw new IllegalStateException("Listener already registed");
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
