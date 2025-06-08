package org.zephyrsoft.optigemspoonfeeder.ui;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.service.PersonService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.textfield.TextField;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class PersonDialog extends Dialog {

    public PersonDialog(OptigemSpoonfeederProperties.AccountProperties accountProperties, Table tableOptigemAccounts,
        PersonService personService, int initialNummer, String vorname, String nachname, String iban,
        Consumer<Integer> thisNumberWasCreated) {
        setWidth("65%");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        setHeaderTitle("Person und Unterkonto anlegen");
        Button closeButton = new Button(new Icon("lumo", "cross"),
            (e) -> close());
        closeButton.setTooltipText("Schließen ohne Speichern");
        closeButton.setTabIndex(-1);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        TextField nummerField = new TextField();
        nummerField.setId("nummerField");
        nummerField.setWidthFull();
        nummerField.setValue(String.valueOf(initialNummer));

        TextField vornameField = new TextField();
        vornameField.setId("vornameField");
        vornameField.setWidthFull();
        vornameField.setValue(vorname);

        TextField nachnameField = new TextField();
        nachnameField.setId("nachnameField");
        nachnameField.setWidthFull();
        nachnameField.setValue(nachname);

        TextField ibanField = new TextField();
        ibanField.setId("ibanField");
        ibanField.setWidthFull();
        ibanField.setValue(iban);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.addClassName("spaced-form");
        add(formLayout);

        formLayout.addFormItem(nummerField, "Personen-Nummer");
        formLayout.addFormItem(vornameField, "Vorname");
        formLayout.addFormItem(nachnameField, "Nachname");
        formLayout.addFormItem(ibanField, "IBAN");

        Button saveButton = new Button("Anlegen", e -> {
            try {
                int createdNumber = personService.createPersonAndUnterkonto(accountProperties, tableOptigemAccounts,
                    Integer.parseInt(nummerField.getValue()), vornameField.getValue(), nachnameField.getValue(), ibanField.getValue());
                MessageDialog.show("Person angelegt",
                    "Person \"" + Stream.of(vornameField.getValue(), nachnameField.getValue()).filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(" ")) + "\" mit Nummer " + createdNumber + " angelegt.");
                thisNumberWasCreated.accept(createdNumber);
                close();
            } catch (Exception ex) {
                MessageDialog.show("Fehler", ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(saveButton);
    }
}
