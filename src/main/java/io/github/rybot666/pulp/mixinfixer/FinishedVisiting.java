package io.github.rybot666.pulp.mixinfixer;

public class FinishedVisiting extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
