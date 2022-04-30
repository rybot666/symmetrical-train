package io.github.rybot666.pulp.mixin_backend.transformer.transformations;

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
public abstract class Transformation {
    /**
     * Applies the transformation to the base and proxy classes
     *
     * @param state the state from the transformer
     * @return whether the transformation modified any classes
     */
    public boolean apply(TransformationState state) {
        return false;
    }

    // TODO handle class transformation of classes outside of the proxy and base classes

}
