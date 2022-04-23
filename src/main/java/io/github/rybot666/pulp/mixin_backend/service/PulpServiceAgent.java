package io.github.rybot666.pulp.mixin_backend.service;

import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.IConsumer;

import java.util.Collection;

@SuppressWarnings("unused") // Used by Mixin via reflection - class name is in PulpMixinService
public class PulpServiceAgent implements IMixinPlatformServiceAgent {
    @Override
    public void init() {

    }

    @Override
    public String getSideName() {
        return Constants.SIDE_DEDICATEDSERVER;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"}) // I don't know why IntelliJ doesn't like this
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {

    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void unwire() {

    }

    @Override
    public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        return AcceptResult.ACCEPTED;
    }

    @Override
    public String getPhaseProvider() {
        return null;
    }

    @Override
    public void prepare() {

    }

    @Override
    public void initPrimaryContainer() {

    }

    @Override
    public void inject() {

    }
}
