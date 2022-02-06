package io.github.rybot666.symmetricaltrain.mixinbackend;

import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClassProvider implements IClassProvider, IClassBytecodeProvider {
    private final MixinService service;

    public ClassProvider(MixinService service) {
        this.service = service;
    }

    @Override
    public URL[] getClassPath() {
        throw new UnsupportedOperationException("Cannot get class path on spigot because bad");
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, this.service.loader);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, this.service.loader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, this.service.loader);
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
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

        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        // TODO: handle transformers?
        return getClassNode(name);
    }
}
