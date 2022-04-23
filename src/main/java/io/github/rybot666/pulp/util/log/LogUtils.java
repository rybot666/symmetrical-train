package io.github.rybot666.pulp.util.log;

import io.github.rybot666.pulp.util.log.PulpLogger;

public class LogUtils {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private LogUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static PulpLogger getLogger() {
        Class<?> caller = STACK_WALKER.getCallerClass();
        return new PulpLogger(caller, "Pulp");
    }

    public static PulpLogger getLogger(String name) {
        Class<?> caller = STACK_WALKER.getCallerClass();
        return new PulpLogger(caller, String.format("Pulp/%s", name));
    }
}
