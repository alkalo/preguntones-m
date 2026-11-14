package io.papermc.paper.registry.data.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilder;
import io.papermc.paper.registry.data.dialog.type.DialogType;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogRegistryEntry {
    interface Builder extends DialogRegistryEntry, RegistryBuilder<Dialog> {
        Builder base(DialogBase base);

        Builder type(DialogType type);
    }
}
