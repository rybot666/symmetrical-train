package io.github.rybot666.pulp.mixin_backend.transformer.transformations;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.ProxyFactory;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class TransformationState {
    public final ClassNode original;
    public final ClassNode transformed;

    public final ClassNode proxy;

    public final List<MethodNode> addedMethods;
    public final List<String> addedInterfaces;

    public TransformationState(ClassNode original, ClassNode transformed) {
        this.original = original;
        this.transformed = transformed;

        this.addedMethods = new ArrayList<>(transformed.methods);

        for (MethodNode method : original.methods) {
            this.addedMethods.removeIf(m -> m.name.equals(method.name) && m.desc.equals(method.desc));
        }

        this.addedInterfaces = new ArrayList<>(transformed.interfaces);
        this.addedInterfaces.removeAll(original.interfaces);

        this.proxy = ProxyFactory.generateBaseProxy(this.transformed);
    }
}
