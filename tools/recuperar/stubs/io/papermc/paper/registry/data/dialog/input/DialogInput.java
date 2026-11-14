package io.papermc.paper.registry.data.dialog.input;

import net.kyori.adventure.text.Component;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogInput {
    static TextDialogInput.Builder text(String key, Component label) {
        throw new UnsupportedOperationException();
    }
}
