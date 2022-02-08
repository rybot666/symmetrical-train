package io.github.rybot666.pulp.mixinservice;

import io.github.rybot666.pulp.PulpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.*;

import java.io.InputStream;
import java.util.Collection;

public class PulpMixinService extends MixinServiceAbstract {
    public static final String NAME = PulpPlugin.NAME.concat("/").concat(Bukkit.getName());

    final PulpHackyClassLoader hackyClassLoader;
    private final PulpClassProvider classProvider;

    public PulpMixinService(PulpPlugin owner) {
        this.hackyClassLoader = new PulpHackyClassLoader(this.getClass().getClassLoader(), (JavaPluginLoader) owner.getPluginLoader());
        this.classProvider = new PulpClassProvider(this);
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
        return null;
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return this.hackyClassLoader.getResourceAsStream(name);
    }
}
