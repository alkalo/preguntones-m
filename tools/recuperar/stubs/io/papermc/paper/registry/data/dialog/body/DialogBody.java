package io.papermc.paper.registry.data.dialog.body;

import net.kyori.adventure.text.Component;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogBody {
    static PlainMessageDialogBody plainMessage(Component message, int width) {
        throw new UnsupportedOperationException();
    }
}
