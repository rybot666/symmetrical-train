package io.github.rybot666.symmetricaltrain.mixinbackend;

import io.github.rybot666.symmetricaltrain.SymmetricalTrain;
import org.bukkit.plugin.java.HackyPluginClassLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.*;

import java.io.InputStream;
import java.util.Collection;

public class MixinService extends MixinServiceAbstract {
    final HackyPluginClassLoader loader;
    private final ClassProvider classProvider;

    public MixinService(SymmetricalTrain owner) {
        this.loader = new HackyPluginClassLoader(this.getClass().getClassLoader(), (JavaPluginLoader) owner.getPluginLoader());
        this.classProvider = new ClassProvider(this);
    }

    @Override
    public String getName() {
        return "SymmetricalTrain";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this.classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
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
        return null;
    }
}
