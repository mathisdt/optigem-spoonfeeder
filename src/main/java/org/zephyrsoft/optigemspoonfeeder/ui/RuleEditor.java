package org.zephyrsoft.optigemspoonfeeder.ui;

import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class RuleEditor extends Dialog {
    public RuleEditor(PersistenceService persistenceService) {
        setWidth("65%");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        setHeaderTitle("Regeln bearbeiten");
        Button closeButton = new Button(new Icon("lumo", "cross"),
            (e) -> close());
        closeButton.setTooltipText("Schließen ohne Speichern");
        closeButton.setTabIndex(-1);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        TextArea rulesEditor = new TextArea();
        rulesEditor.addClassName("codefont");
        rulesEditor.setWidthFull();
        rulesEditor.setMinHeight("200px");
        rulesEditor.setValue(persistenceService.getRules());

        Button saveButton = new Button("Speichern", e -> {
            try {
                persistenceService.saveRules(rulesEditor.getValue());
                MessageDialog.show("Regeln geändert",
                    "Die angepassten Regeln wurden gespeichert.");
                close();
            } catch (Exception ex) {
                MessageDialog.show("Fehler", ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        VerticalLayout layout = new VerticalLayout(rulesEditor, saveButton);
        layout.setSizeFull();
        add(layout);
    }
}
