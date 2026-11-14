package io.papermc.paper.dialog;

import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import java.util.function.Consumer;
import net.kyori.adventure.dialog.DialogLike;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface Dialog extends DialogLike {
    static Dialog create(Consumer<RegistryBuilderFactory<Dialog, ? extends DialogRegistryEntry.Builder>> consumer) {
        throw new UnsupportedOperationException();
    }
}
