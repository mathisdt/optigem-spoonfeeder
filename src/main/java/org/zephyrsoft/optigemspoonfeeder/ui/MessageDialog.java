package org.zephyrsoft.optigemspoonfeeder.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;

public class MessageDialog extends Dialog {

    public MessageDialog(String title, String message) {
        setWidth("45%");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        setHeaderTitle(title);
        Button closeButton = new Button(new Icon("lumo", "cross"),
            e -> close());
        closeButton.setTooltipText("Schließen");
        closeButton.setTabIndex(-1);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        add(new Span(message));
    }

    public static void show(String title, String message) {
        new MessageDialog(title, message).open();
    }
}
