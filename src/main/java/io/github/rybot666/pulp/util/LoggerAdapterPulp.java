package io.github.rybot666.pulp.util;

import io.github.rybot666.pulp.PulpPlugin;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This code is copied from LoggerAdapterJava, because it provides no way to override the creation of the logger
 */
public class LoggerAdapterPulp extends LoggerAdapterAbstract {
    private static final java.util.logging.Level[] LEVELS = {
            /* FATAL = */ java.util.logging.Level.SEVERE,
            /* ERROR = */ java.util.logging.Level.SEVERE,
            /* WARN =  */ java.util.logging.Level.WARNING,
            /* INFO =  */ java.util.logging.Level.INFO,
            /* DEBUG = */ java.util.logging.Level.FINE,
            /* TRACE = */ java.util.logging.Level.FINER
    };

    private final Logger logger;

    public LoggerAdapterPulp(String name) {
        super(name);
        this.logger = LoggerAdapterPulp.getLogger(name);
    }

    @Override
    public String getType() {
        return "Pulp Log Adapter";
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.warn("Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

    @Override
    public void debug(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.fine(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.fine(formatted.getThrowable().toString());
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        this.logger.fine(message);
        this.logger.fine(t.toString());
    }

    @Override
    public void error(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void error(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void fatal(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void info(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.info(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.info(formatted.getThrowable().toString());
        }
    }

    @Override
    public void info(String message, Throwable t) {
        this.logger.info(message);
        this.logger.info(t.toString());
    }

    @Override
    public void log(Level level, String message, Object... params) {
        java.util.logging.Level logLevel = LoggerAdapterPulp.LEVELS[level.ordinal()];
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.log(logLevel, formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.log(LoggerAdapterPulp.LEVELS[level.ordinal()], formatted.getThrowable().toString());
        }
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        java.util.logging.Level logLevel = LoggerAdapterPulp.LEVELS[level.ordinal()];
        this.logger.log(logLevel, message);
        this.logger.log(logLevel, t.toString());
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        this.warn("Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
        return t;
    }

    @Override
    public void trace(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.finer(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.finer(formatted.getThrowable().toString());
        }
    }

    @Override
    public void trace(String message, Throwable t) {
        this.logger.finer(message);
        this.logger.finer(t.toString());
    }

    @Override
    public void warn(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.warning(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.warning(formatted.getThrowable().toString());
        }
    }

    @Override
    public void warn(String message, Throwable t) {
        this.logger.warning(message);
        this.logger.warning(t.toString());
    }

    private static Logger getLogger(String name) {
        return new PulpLogger(PulpPlugin.class, String.format("Pulp/%s", name), PulpPlugin.LOGGER);
    }
}
