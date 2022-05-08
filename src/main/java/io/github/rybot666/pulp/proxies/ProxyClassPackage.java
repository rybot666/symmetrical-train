package io.github.rybot666.pulp.proxies;

import java.lang.invoke.MethodHandles;

public class ProxyClassPackage {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ProxyClassPackage() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }
}
