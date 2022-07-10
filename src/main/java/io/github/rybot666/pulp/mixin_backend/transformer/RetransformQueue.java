package io.github.rybot666.pulp.mixin_backend.transformer;

import io.github.rybot666.pulp.PulpBootstrap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class RetransformQueue {
    private Set<Class<?>> current = new HashSet<>();
    private Set<Class<?>> next = new HashSet<>();

    private int stage = 0;

    public boolean isCurrentEmpty() {
        return this.current.isEmpty();
    }

    public boolean isNextEmpty() {
        return this.next.isEmpty();
    }

    public void handleRetransformationThreads(Consumer<Class<?>> consumer) {
        while (this.current.isEmpty() && !this.next.isEmpty()) {
            Set<Class<?>> targets = this.shift();

            Thread thread = new Thread(() -> targets.forEach(consumer));
            thread.start();

            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread was unexpectedly interrupted", e);
            }
        }
    }

    public Set<Class<?>> shift() {
        this.stage++;

        this.current = this.next;
        this.next = new HashSet<>();

        return this.current;
    }

    public void push(Class<?> clazz) {
        this.next.add(clazz);
    }

    public void pushAll(Collection<? extends Class<?>> collection) {
        this.next.addAll(collection);
    }

    public void pushAllLoaded(Collection<String> names) {
        for (Class<?> clazz: PulpBootstrap.INSTRUMENTATION.getAllLoadedClasses()) {
            if (clazz.isArray() || clazz.isPrimitive() || clazz.isSynthetic() || !PulpBootstrap.INSTRUMENTATION.isModifiableClass(clazz)) {
                continue;
            }

            if (names.contains(clazz.getName().replace('.', '/'))) {
                this.push(clazz);
            }
        }
    }

    public int getStage() {
        return this.stage;
    }
}
