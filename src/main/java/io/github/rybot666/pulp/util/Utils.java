package io.github.rybot666.pulp.util;

import com.google.common.io.Closeables;
import io.github.rybot666.pulp.util.log.LogUtils;
import io.github.rybot666.pulp.util.log.PulpLogger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class Utils {
    private static final PulpLogger LOGGER = LogUtils.getLogger();

    private Utils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static ClassNode readNode(ClassReader reader) {
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    @Nullable
    public static ClassReader getClassReader(ClassLoader loader, String name) throws ClassNotFoundException, IOException {
        InputStream classStream = null;
        ClassReader reader;

        try {
            final String resourcePath = name.replace('.', '/').concat(".class");
            classStream = loader == null ? Object.class.getResourceAsStream("/" + resourcePath) : loader.getResourceAsStream(resourcePath);

            if (classStream == null) {
                //LOGGER.warning(() -> String.format("Missing class bytecode for %s", name));
                return null;
            }

            reader = new ClassReader(classStream);
        } finally {
            //noinspection UnstableApiUsage
            Closeables.closeQuietly(classStream);
        }

        return reader;
    }

    public static ClassNode readNode(byte[] data) {
        ClassReader reader = new ClassReader(data);
        ClassNode node = new ClassNode();

        reader.accept(node, ClassReader.EXPAND_FRAMES);

        return node;
    }

    public static <T> T make(T obj, Consumer<T> initializer) {
        initializer.accept(obj);
        return obj;
    }
}
