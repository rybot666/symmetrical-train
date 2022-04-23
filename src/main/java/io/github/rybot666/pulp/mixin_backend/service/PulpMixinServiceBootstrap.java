package io.github.rybot666.pulp.mixin_backend.service;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class PulpMixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return PulpMixinService.NAME;
    }

    @Override
    public String getServiceClassName() {
        return "io.github.rybot666.pulp.mixin_backend.service.PulpMixinService";
    }

    @Override
    public void bootstrap() {

    }
}
