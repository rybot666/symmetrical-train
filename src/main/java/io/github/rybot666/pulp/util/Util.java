package io.github.rybot666.pulp.util;

import com.google.common.io.Closeables;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;

public class Util {
    private Util() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static ClassNode readNode(ClassReader reader) {
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    public static ClassReader getClassReader(ClassLoader loader, String name) throws ClassNotFoundException, IOException {
        InputStream classStream = null;
        ClassReader reader;

        try {
            final String resourcePath = name.replace('.', '/').concat(".class");
            classStream = loader.getResourceAsStream(resourcePath);
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
