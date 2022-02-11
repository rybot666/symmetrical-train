package io.github.rybot666.pulp.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class PulpLogger extends Logger {
    private final String prefix;

    public PulpLogger(Class<? extends Plugin> pluginClazz, String name) {
        super(pluginClazz.getCanonicalName(), null);
        this.setParent(Bukkit.getLogger());
        this.setLevel(Level.ALL);

        this.prefix = String.format("[%s] ", name);
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(this.prefix + logRecord.getMessage());
        super.log(logRecord);
    }
}