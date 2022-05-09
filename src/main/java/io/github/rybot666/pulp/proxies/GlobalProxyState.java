package io.github.rybot666.pulp.proxies;

import io.github.rybot666.pulp.mixin_backend.transformer.proxy.ProxyMarker;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class GlobalProxyState {
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // Map of internal target name to proxy class info
    public static final Map<String, Class<? extends ProxyMarker>> PROXY_CLASS_MAP = new HashMap<>();

    private GlobalProxyState() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }
}
