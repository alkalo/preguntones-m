package io.papermc.paper.registry.data.dialog.action;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickEvent;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogAction {
    interface CustomClickAction extends DialogAction {
    }

    interface StaticAction extends DialogAction {
    }

    static CustomClickAction customClick(Key key, BinaryTagHolder tag) {
        throw new UnsupportedOperationException();
    }

    static StaticAction staticAction(ClickEvent event) {
        throw new UnsupportedOperationException();
    }
}
