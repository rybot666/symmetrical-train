package io.github.rybot666.pulp.mixinservice;

import org.bukkit.plugin.java.JavaPluginLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Super cursed class loader that delegates to the classloader of every plugin in a plugin loader
 *
 * Placed here for repackaging hack on `PluginClassLoader` and its methods
 */
public class PulpHackyClassLoader extends URLClassLoader {
    private static final VarHandle LOADERS_HANDLE;
    private static final Class<?> PLUGIN_CLASS_LOADER_CLASS;
    private static final MethodHandle LOAD_CLASS_0_HANDLE;
    private final JavaPluginLoader loader;

    static {
        try {
            LOADERS_HANDLE = MethodHandles.privateLookupIn(JavaPluginLoader.class, MethodHandles.lookup())
                    .findVarHandle(JavaPluginLoader.class, "loaders", List.class);

            PLUGIN_CLASS_LOADER_CLASS = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
            LOAD_CLASS_0_HANDLE = MethodHandles.privateLookupIn(PLUGIN_CLASS_LOADER_CLASS, MethodHandles.lookup())
                    .findVirtual(PLUGIN_CLASS_LOADER_CLASS, "loadClass0", MethodType.methodType(Class.class, String.class, boolean.class, boolean.class, boolean.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get the `loaders` var handle of `JavaPluginLoader`", e);
        }
    }

    public PulpHackyClassLoader(ClassLoader parent, JavaPluginLoader loader) {
        super(new URL[0], parent);

        this.loader = loader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        List<Object> loaders = (List<Object>) LOADERS_HANDLE.get(this.loader);

        for (Object loader : loaders) {
            try {
                return (Class<?>) LOAD_CLASS_0_HANDLE.invoke(loader, name, true, false, true);
            } catch (ClassNotFoundException ignored) {

            } catch (Throwable t) {
                throw new RuntimeException("Unexpected throwable", t);
            }
        }

        return super.findClass(name);
    }
}
