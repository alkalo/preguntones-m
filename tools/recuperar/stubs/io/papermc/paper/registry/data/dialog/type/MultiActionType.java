package io.papermc.paper.registry.data.dialog.type;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface MultiActionType extends DialogType {
    interface Builder {
        Builder columns(int columns);

        MultiActionType build();
    }
}
