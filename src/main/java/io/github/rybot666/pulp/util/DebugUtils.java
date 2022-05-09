package io.github.rybot666.pulp.util;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DebugUtils {
    private DebugUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static void dumpClass(Path target, ClassNode node) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);

        Files.createDirectories(target.getParent());

        Files.deleteIfExists(target);
        Files.createFile(target);

        Files.write(target, writer.toByteArray(), StandardOpenOption.WRITE);
    }

    public static void checkAndDumpTrace(Path target, ClassNode node) throws IOException {
        Files.createDirectories(target.getParent());

        Files.deleteIfExists(target);
        Files.createFile(target);

        CheckClassAdapter cca = new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(new FileWriter(target.toFile()))));
        node.accept(cca);
    }
}
