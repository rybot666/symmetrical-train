package io.github.rybot666.pulp.mixinservice;

import io.github.rybot666.pulp.PulpPlugin;
import io.github.rybot666.pulp.instrumentation.ClassLoadWatchTransformer;
import io.github.rybot666.pulp.mixinfixer.MixinFixer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterJava;
import org.spongepowered.asm.service.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class PulpMixinService extends MixinServiceAbstract {
    public static final String NAME = PulpPlugin.NAME.concat("/").concat(Bukkit.getName());
    private static final String JAVA_PLUGIN_LOADER_PATTERN = "\\.jar$";

    final PulpHackyClassLoader hackyClassLoader;
    private final PulpClassProvider classProvider;
    private final MixinFixer fixer;
    private final IContainerHandle primaryContainer;

    public PulpMixinService() {
        this.hackyClassLoader = new PulpHackyClassLoader(this.getClass().getClassLoader(), getPluginLoader());
        this.classProvider = new PulpClassProvider(this);
        this.fixer = new MixinFixer(new PulpMixinFixerContext(this));
        try {
            this.primaryContainer = new ContainerHandleURI(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ClassLoadWatchTransformer.register(PulpPlugin.INSTRUMENTATION, this.fixer::registerClass);
    }

    @SuppressWarnings("unchecked")
    private static JavaPluginLoader getPluginLoader() {
        try {
            SimplePluginManager manager = ((SimplePluginManager) Bukkit.getPluginManager());

            Field fileAssociationsField = SimplePluginManager.class.getDeclaredField("fileAssociations");
            fileAssociationsField.setAccessible(true);
            Map<Pattern, PluginLoader> fileAssociations = (Map<Pattern, PluginLoader>) fileAssociationsField.get(manager);

            for (Map.Entry<Pattern, PluginLoader> entry : fileAssociations.entrySet()) {
                if (entry.getKey().pattern().equals(JAVA_PLUGIN_LOADER_PATTERN)) {
                    return (JavaPluginLoader) entry.getValue();
                }
            }

            throw new AssertionError("Couldn't find JavaPluginLoader in file associations");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to reflect JavaPluginLoader", e);
        }
    }

    @Override
    public String getName() {
        return PulpMixinService.NAME;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public PulpClassProvider getClassProvider() {
        return this.classProvider;
    }

    @Override
    public PulpClassProvider getBytecodeProvider() {
        return this.classProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return primaryContainer;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return this.hackyClassLoader.getResourceAsStream(name);
    }

    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterJava("Mixin");
    }
}
