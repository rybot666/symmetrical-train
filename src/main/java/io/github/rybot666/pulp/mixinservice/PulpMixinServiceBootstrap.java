package io.github.rybot666.pulp.mixinservice;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class PulpMixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return PulpMixinService.NAME;
    }

    @Override
    public String getServiceClassName() {
        return "io.github.rybot666.pulp.mixinservice.PulpMixinService";
    }

    @Override
    public void bootstrap() {

    }
}
