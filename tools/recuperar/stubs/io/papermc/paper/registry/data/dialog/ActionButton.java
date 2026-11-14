package io.papermc.paper.registry.data.dialog;

import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface ActionButton {
    interface Builder {
        Builder width(int width);

        Builder action(DialogAction action);

        ActionButton build();
    }

    static Builder builder(Component label) {
        throw new UnsupportedOperationException();
    }
}
