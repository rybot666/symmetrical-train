package io.github.rybot666.pulp.mixin_backend.service;

import io.github.rybot666.pulp.PulpPlugin;
import io.github.rybot666.pulp.mixin_backend.HackyClassLoader;
import io.github.rybot666.pulp.mixin_backend.fixer.MixinFixer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class PulpMixinService extends MixinServiceAbstract {
    public static final String NAME = PulpPlugin.NAME.concat(" on ").concat(Bukkit.getName());
    private static final String JAVA_PLUGIN_LOADER_PATTERN = "\\.jar$";

    public final HackyClassLoader hackyClassLoader;

    final MixinFixer fixer;
    IMixinTransformer transformer;
    private final PulpClassProvider classProvider;
    private final IContainerHandle primaryContainer;
    private PulpTransformer pulpTransformer;

    public PulpMixinService() {
        this.hackyClassLoader = new HackyClassLoader(this.getClass().getClassLoader(), getPluginLoader());
        this.classProvider = new PulpClassProvider(this);

        this.fixer = new MixinFixer(this);

        try {
            this.primaryContainer = new ContainerHandleURI(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
                    PluginLoader loader = entry.getValue();

                    if (!JavaPluginLoader.class.isAssignableFrom(loader.getClass())) {
                        throw new AssertionError("JAR loader is not a JavaPluginLoader!");
                    }

                    return (JavaPluginLoader) loader;
                }
            }

            throw new AssertionError("Couldn't find JavaPluginLoader in file associations");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to reflect for JavaPluginLoader", e);
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
        return Collections.singleton("io.github.rybot666.pulp.mixin_backend.service.PulpServiceAgent");
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
        if ("mixin".equals(name)) {
            return new LoggerAdapterPulp("Mixin");
        }

        return new LoggerAdapterPulp(String.format("Mixin/%s", name));
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            this.transformer = ((IMixinTransformerFactory) internal).createTransformer();
            this.pulpTransformer = PulpTransformer.register(PulpPlugin.INSTRUMENTATION, this);
        }
    }

    @Override
    public void prepare() {
        this.fixer.interfaceCache.registerAllClasses(PulpPlugin.INSTRUMENTATION);

        super.prepare();
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void beginPhase() {
        super.beginPhase();
    }
}
