package io.github.rybot666.pulp.mixinfixer;

import org.objectweb.asm.tree.ClassNode;

public interface IMixinFixerContext {
    void requestRetransform(Iterable<String> classes);

    boolean isInterface(String internalName);

    void defineProxyClass(ClassNode proxyClass);
}
