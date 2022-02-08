package io.github.rybot666.pulp.mixinservice;

import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;

import java.io.IOException;
import java.io.InputStream;
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
        ClassReader reader = this.getClassReader(name);

        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        // TODO: handle transformers?
        return getClassNode(name);
    }

    public ClassReader getClassReader(String name) throws ClassNotFoundException, IOException {
        InputStream classStream = null;
        ClassReader reader;

        try {
            final String resourcePath = name.replace('.', '/').concat(".class");
            classStream = this.service.getResourceAsStream(resourcePath);
            if (classStream == null) {
                throw new ClassNotFoundException(name);
            }
            reader = new ClassReader(classStream);
        } finally {
            //noinspection UnstableApiUsage
            Closeables.closeQuietly(classStream);
        }

        return reader;
    }
}
