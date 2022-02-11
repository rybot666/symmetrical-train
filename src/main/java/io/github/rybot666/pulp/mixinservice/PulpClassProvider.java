package io.github.rybot666.pulp.mixinservice;

import io.github.rybot666.pulp.util.Util;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;

import java.io.IOException;
import java.net.URL;

public class PulpClassProvider implements IClassProvider, IClassBytecodeProvider {
    private final PulpMixinService service;

    public PulpClassProvider(PulpMixinService service) {
        this.service = service;
    }

    @Override
    public URL[] getClassPath() {
        throw new UnsupportedOperationException("Cannot get class path on spigot because bad");
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, this.service.hackyClassLoader);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, this.service.hackyClassLoader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, PulpClassProvider.class.getClassLoader());
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        ClassReader classReader = Util.getClassReader(this.service.hackyClassLoader, name);
        if (classReader == null) {
            throw new ClassNotFoundException(name);
        }
        return Util.readNode(classReader);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        // TODO: handle transformers?
        return getClassNode(name);
    }
}
