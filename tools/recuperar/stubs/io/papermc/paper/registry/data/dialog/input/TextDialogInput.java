package io.papermc.paper.registry.data.dialog.input;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface TextDialogInput extends DialogInput {
    interface Builder {
        Builder maxLength(int maxLength);

        Builder width(int width);

        Builder initial(String initial);

        TextDialogInput build();
    }
}
