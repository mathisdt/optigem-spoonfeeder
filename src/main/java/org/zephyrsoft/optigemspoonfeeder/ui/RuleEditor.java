package org.zephyrsoft.optigemspoonfeeder.ui;

import org.zephyrsoft.optigemspoonfeeder.model.RuleValidationResult;
import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;
import org.zephyrsoft.optigemspoonfeeder.service.RuleService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class RuleEditor extends Dialog {
    public RuleEditor(RuleService ruleService, PersistenceService persistenceService) {
        setWidth("75%");
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
        rulesEditor.setAutocorrect(false);
        rulesEditor.setValue(persistenceService.getRules());

        Pre error = new Pre();
        error.setSizeFull();
        Scroller scrollingErrorDiv = new Scroller(error);
        scrollingErrorDiv.setWidthFull();
        scrollingErrorDiv.setMaxHeight("100px");
        error.addClassNames("codefont", "red", "no-background");

        Button saveButton = new Button("Prüfen & Speichern", e -> {
            String rules = rulesEditor.getValue();
            RuleValidationResult validationResult = ruleService.validateRules(rules);
            if (validationResult.isError()) {
                error.setText(validationResult.getErrorMessage());
                if (validationResult.getErrorLine() > 0) {
                    long index = 0;
                    if (validationResult.getErrorLine() > 1) {
                        String[] rulesLines = rules.split("\n", (int)validationResult.getErrorLine() - 1);
                        index = rules.length() - rulesLines[rulesLines.length - 1].length();
                    }
                    rulesEditor.focus();
                    rulesEditor.getElement().executeJs("this.inputElement.setSelectionRange(" + index + "," + index + ");");
                }
            } else {
                try {
                    persistenceService.saveRules(rules);
                    MessageDialog.show("Regeln geändert",
                        "Die angepassten Regeln wurden gespeichert.");
                    close();
                } catch (Exception ex) {
                    MessageDialog.show("Fehler", ex.getMessage());
                }
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        VerticalLayout layout = new VerticalLayout(rulesEditor, new HorizontalLayout(saveButton, scrollingErrorDiv));
        layout.setSizeFull();
        add(layout);
    }
}
