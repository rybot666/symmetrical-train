package io.github.rybot666.pulp.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Lazy<T> {
    private final Supplier<T> supplier;
    private T value;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (this.value == null) {
            this.value = this.supplier.get();
        }

        return this.value;
    }

    public void applyOptional(Consumer<T> consumer) {
        if (this.value != null) {
            consumer.accept(this.value);
        }
    }
}
