package io.github.rybot666.pulp.mixin_backend.transformer.fixer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The main operation Pulp needs to perform
 *
 * Takes the original class node that we input to mixin, and the one it returns to us, and generates a diff between
 * them. This is required because Java's instrumentation API is heavily restricted in the operations it can perform.
 *
 * The full restrictions are specified in the documentation for the instrumentation class (emphasis mine)
 * <blockquote>The retransformation may change <b>method bodies, the constant pool and attributes</b>. The
 * retransformation must <b>not add, remove or rename fields or methods</b>, change the signatures of methods, or
 * change inheritance</b>. These restrictions maybe be lifted in future versions.</blockquote>
 *
 * These restrictions are quite annoying for mixin - most injectors generate a new method and call it from within
 * the body of the method, and adding interfaces to classes in this way is very difficult. Definalising a field is
 * harder still. However, all the above are possible by generating a secondary proxy class, and in some cases
 * modifying usages of the target class, which is what Pulp aims to provide.
 */
public class ClassDiff {
    private final ObjectDiff<MethodNode> methods;
    private final ObjectDiff<FieldNode> fields;
    private final ObjectDiff<String> interfaces;

    public ClassDiff(ClassNode original, ClassNode transformed) {
        this.methods = new ObjectDiff<>(original.methods, transformed.methods);
        this.fields = new ObjectDiff<>(original.fields, transformed.fields);
        this.interfaces = new ObjectDiff<>(original.interfaces, transformed.interfaces);
    }

    public static class ObjectDiff<T> {
        public final List<T> added;
        public final List<T> removed;

        public ObjectDiff(List<T> original, List<T> modified) {
            this.added = new ArrayList<>(modified);
            this.added.removeAll(original);

            this.removed = new ArrayList<>(original);
            this.removed.removeAll(modified);
        }
    }
}
