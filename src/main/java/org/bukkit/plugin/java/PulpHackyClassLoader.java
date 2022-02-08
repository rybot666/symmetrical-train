package org.bukkit.plugin.java;

import java.lang.invoke.MethodHandles;
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
    private final JavaPluginLoader loader;

    static {
        try {
            LOADERS_HANDLE = MethodHandles.privateLookupIn(JavaPluginLoader.class, MethodHandles.lookup())
                    .findVarHandle(JavaPluginLoader.class, "loaders", List.class);
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
        List<PluginClassLoader> loaders = (List<PluginClassLoader>) LOADERS_HANDLE.get(this.loader);

        for (PluginClassLoader loader : loaders) {
            try {
                return loader.loadClass0(name, true, false, true);
            } catch (ClassNotFoundException ignored) {}
        }

        return super.findClass(name);
    }
}
