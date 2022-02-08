package io.github.rybot666.pulp.mixinservice;

import io.github.rybot666.pulp.mixinfixer.IMixinFixerContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;

public class PulpMixinFixerContext implements IMixinFixerContext {
    private final PulpMixinService service;

    public PulpMixinFixerContext(PulpMixinService service) {
        this.service = service;
    }

    @Override
    public void requestRetransform(Iterable<String> classes) {
        throw new RuntimeException("Fuck you");
    }

    @Override
    public boolean isInterface(String internalName) {
        try {
            ClassReader reader = this.service.getBytecodeProvider().getClassReader(internalName);
            return (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
        } catch (ClassNotFoundException | IOException ignored) {}

        return false;
    }
}