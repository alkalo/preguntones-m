package io.papermc.paper.registry.data.dialog.type;

import io.papermc.paper.registry.data.dialog.ActionButton;
import java.util.List;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogType {
    static MultiActionType.Builder multiAction(List<ActionButton> actions) {
        throw new UnsupportedOperationException();
    }
}
