package io.github.rybot666.pulp.util.log;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class PulpLogger extends Logger {
    private final String prefix;

    public PulpLogger(Class<?> clazz, String name, Logger parent) {
        super(clazz.getCanonicalName(), null);
        this.setParent(parent);
        this.setLevel(Level.ALL);

        this.prefix = String.format("[%s] ", name);
    }

    public PulpLogger(Class<?> clazz, String name) {
        this(clazz, name, Bukkit.getLogger());
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(this.prefix + logRecord.getMessage());
        super.log(logRecord);
    }
}