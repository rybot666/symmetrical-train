package io.github.rybot666.pulp.mixin_backend;

import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Super cursed class loader that delegates to the classloader of every plugin in a plugin loader
 */
public class HackyClassLoader extends URLClassLoader {
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

    public HackyClassLoader(ClassLoader parent, JavaPluginLoader loader) {
        super(new URL[0], parent);

        this.loader = loader;
    }

    @SuppressWarnings("unchecked")
    private List<URLClassLoader> getLoaders() {
        return (List<URLClassLoader>) LOADERS_HANDLE.get(this.loader);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (URLClassLoader loader: this.getLoaders()) {
            try {
                return (Class<?>) LOAD_CLASS_0_HANDLE.invoke(loader, name, true, false, true);
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {

            } catch (Throwable th) {
                throw new RuntimeException("Downstream plugin classloader threw exception in loadClass0", th);
            }
        }

        return super.findClass(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (URLClassLoader loader: this.getLoaders()) {
            InputStream resource = loader.getResourceAsStream(name);

            if (resource == null) {
                continue;
            }

            return resource;
        }

        return super.getResourceAsStream(name);
    }
}
