package io.github.rybot666.pulp.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class PulpLogger extends Logger {
    private final String prefix;

    public PulpLogger(Class<? extends Plugin> pluginClazz, String name, Logger parent) {
        super(pluginClazz.getCanonicalName(), null);
        this.setParent(parent);
        this.setLevel(Level.ALL);

        this.prefix = String.format("[%s] ", name);
    }

    public PulpLogger(Class<? extends Plugin> pluginClazz, String name) {
        this(pluginClazz, name, Bukkit.getLogger());
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(this.prefix + logRecord.getMessage());
        super.log(logRecord);
    }
}