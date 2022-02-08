package io.github.rybot666.pulp.mixinfixer;

public interface IMixinFixerContext {
    void requestRetransform(Iterable<String> classes);

    boolean isInterface(String internalName);
}
