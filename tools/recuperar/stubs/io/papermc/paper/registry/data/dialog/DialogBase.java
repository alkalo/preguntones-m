package io.papermc.paper.registry.data.dialog;

import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import java.util.List;
import net.kyori.adventure.text.Component;

/** Solo para compilar. En el servidor manda la clase de Paper. */
public interface DialogBase {
    enum DialogAfterAction {
        CLOSE, NONE, WAIT_FOR_RESPONSE
    }

    interface Builder {
        Builder canCloseWithEscape(boolean canCloseWithEscape);

        Builder pause(boolean pause);

        Builder afterAction(DialogAfterAction afterAction);

        Builder body(List<? extends DialogBody> body);

        Builder inputs(List<? extends DialogInput> inputs);

        DialogBase build();
    }

    static Builder builder(Component title) {
        throw new UnsupportedOperationException();
    }
}
