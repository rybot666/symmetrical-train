package io.github.rybot666.pulp.util;

import com.google.common.io.Closeables;
import io.github.rybot666.pulp.PulpPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class Util {
    private static final sun.misc.Unsafe UNSAFE;
    static {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to get unsafe", e);
        }
    }

    private Util() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static Object getFieldWithUnsafe(Object instance, Class<?> ownerClass, String fieldName) throws NoSuchFieldException {
        return UNSAFE.getObject(instance, UNSAFE.objectFieldOffset(ownerClass.getDeclaredField(fieldName)));
    }

    public static boolean getBooleanWithUnsafe(Object instance, Class<?> ownerClass, String fieldName) throws NoSuchFieldException {
        return UNSAFE.getBoolean(instance, UNSAFE.objectFieldOffset(ownerClass.getDeclaredField(fieldName)));
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
                PulpPlugin.LOGGER.log(Level.FINEST, () -> String.format("No location found for class \"%s\"", name));
                return null;
            }
            reader = new ClassReader(classStream);
        } finally {
            //noinspection UnstableApiUsage
            Closeables.closeQuietly(classStream);
        }

        return reader;
    }
}
