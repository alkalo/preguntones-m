package io.papermc.paper.registry;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface RegistryBuilderFactory<T, B extends RegistryBuilder<T>> {
    B empty();
}
