package org.zephyrsoft.optigemspoonfeeder.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.Holder;
import org.zephyrsoft.optigemspoonfeeder.model.IdAndName;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;
import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("NonSerializableFieldInSerializableClass")
final class EditDialog extends Dialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(Locale.GERMAN);
    private static final Pattern EVERYTHING_AFTER_SPACE = Pattern.compile(" .*$");

    static {
        CURRENCY_FORMAT.setMinimumIntegerDigits(1);
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setGroupingUsed(true);
    }

    private final Table tableOptigemAccounts;
    private final String accountsColumnHk;
    private final String accountsColumnUk;
    private final String accountsColumnBez;
    private final Table tableOptigemProjects;
    private final String projectsColumnNr;
    private final String projectsColumnBez;
    private final RuleResult rr;
    private final Holder<Boolean> automaticFocusChangeAllowed = new Holder<>(true);
    private final Holder<Boolean> openHauptkontoComboBox = new Holder<>(true);
    private final Binder<RuleResult> binder;
    private final HorizontalLayout betragLayout;
    private final HorizontalLayout hauptkontoLayout;
    private final HorizontalLayout unterkontoLayout;
    private final HorizontalLayout projektLayout;
    private final HorizontalLayout buchungstextLayout;
    private final Button saveButton;
    private Runnable hauptkontoValueChangeTask;

    @SuppressWarnings("unchecked")
    public EditDialog(Table tableOptigemAccounts, String accountsColumnHk, String accountsColumnUk, String accountsColumnBez,
        Table tableOptigemProjects, String projectsColumnNr, String projectsColumnBez,
        String tablePersons, String personsColumnNr, String personsColumnIban, String personsColumnVorname, String personsColumnNachname,
        String accountsHkForPersons,
        RuleResult rr, Runnable updateTableRow, PersistenceService persistenceService) {
        this.tableOptigemAccounts = tableOptigemAccounts;
        this.accountsColumnHk = accountsColumnHk;
        this.accountsColumnUk = accountsColumnUk;
        this.accountsColumnBez = accountsColumnBez;
        this.tableOptigemProjects = tableOptigemProjects;
        this.projectsColumnNr = projectsColumnNr;
        this.projectsColumnBez = projectsColumnBez;
        this.rr = rr;

        setWidth("65%");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        setHeaderTitle("Buchung bearbeiten");
        Button closeButton = new Button(new Icon("lumo", "cross"),
            (e) -> close());
        closeButton.setTooltipText("Schließen ohne Speichern");
        closeButton.setTabIndex(-1);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        Span datum = new Span(DATE_FORMAT.format(rr.getInput().getValutaDatum()));
        Span kontonummer = new Span(rr.getInput().getKontoNummer());
        Span name = new Span(rr.getInput().getName());
        Span verwendungszweck = new Span(rr.getInput().getVerwendungszweckCleanOneline());
        Span betrag = new Span(CURRENCY_FORMAT.format(rr.getInput().getBetragMitVorzeichen()) + " €");
        betrag.addClassName(rr.getInput().isCredit() ? "green" : "red");
        Span buchungstext = new Span(rr.getInput().getBuchungstext());

        TextField betragField = new TextField();
        betragField.setId("betragField");
        betragField.setWidthFull();

        betragLayout = new HorizontalLayout();
        betragLayout.setSizeFull();
        betragLayout.setPadding(false);
        betragLayout.setMargin(false);
        hauptkontoLayout = new HorizontalLayout();
        hauptkontoLayout.setSizeFull();
        hauptkontoLayout.setPadding(false);
        hauptkontoLayout.setMargin(false);
        unterkontoLayout = new HorizontalLayout();
        unterkontoLayout.setSizeFull();
        unterkontoLayout.setPadding(false);
        unterkontoLayout.setMargin(false);
        projektLayout = new HorizontalLayout();
        projektLayout.setSizeFull();
        projektLayout.setPadding(false);
        projektLayout.setMargin(false);
        buchungstextLayout = new HorizontalLayout();
        buchungstextLayout.setSizeFull();
        buchungstextLayout.setPadding(false);
        buchungstextLayout.setMargin(false);

        ComboBox<IdAndName> hauptkontoComboBox = new ComboBox<>();
        hauptkontoComboBox.setId("hauptkontoComboBox");
        hauptkontoComboBox.setAutoOpen(true);
        hauptkontoComboBox.setWidthFull();
        hauptkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        hauptkontoComboBox.addFocusListener(e -> {
            if (openHauptkontoComboBox.getValue()) {
                hauptkontoComboBox.setOpened(true);
            } else {
                openHauptkontoComboBox.setValue(true);
            }
            UI.getCurrent().getPage().executeJs("document.getElementById('hauptkontoComboBox').getElementsByTagName('input')[0].select();");
        });

        ComboBox<IdAndName> unterkontoComboBox = new ComboBox<>();
        unterkontoComboBox.setId("unterkontoComboBox");
        unterkontoComboBox.setAutoOpen(true);
        unterkontoComboBox.setWidthFull();
        unterkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        unterkontoComboBox.addFocusListener(e -> {
            unterkontoComboBox.setOpened(true);
            UI.getCurrent().getPage().executeJs("document.getElementById('unterkontoComboBox').getElementsByTagName('input')[0].select();");
        });

        ComboBox<IdAndName> projektComboBox = new ComboBox<>();
        projektComboBox.setId("projektComboBox");
        projektComboBox.setAutoOpen(true);
        projektComboBox.setWidthFull();
        projektComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        projektComboBox.addFocusListener(e -> {
            projektComboBox.setOpened(true);
            UI.getCurrent().getPage().executeJs("document.getElementById('projektComboBox').getElementsByTagName('input')[0].select();");
        });

        TextField buchungstextField = new TextField();
        buchungstextField.setId("buchungstextField");
        buchungstextField.setWidthFull();

        if (tableOptigemAccounts != null) {
            hauptkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnUk) != null
                    && r.get(accountsColumnUk).equals("0"))
                .map(r -> new IdAndName(Integer.parseInt(r.get(accountsColumnHk)), r.get(accountsColumnBez)))
                .toList()));
            setCalculatedComboboxDropdownWidth(hauptkontoComboBox);
        }

        if (tableOptigemProjects != null) {
            projektComboBox.setItems(new ListDataProvider<>(tableOptigemProjects.getRows().stream()
                .filter(r -> r.get(projectsColumnNr) != null && r.get(projectsColumnBez) != null)
                .map(r -> new IdAndName(Integer.parseInt(r.get(projectsColumnNr)), r.get(projectsColumnBez)))
                .toList()));
            setCalculatedComboboxDropdownWidth(projektComboBox);
        }

        binder = new Binder<>(RuleResult.class);

        binder.forField(betragField).bind(r -> getBetrag(r, 0), (r, b) -> setBetrag(r, 0, b));
        binder.forField(hauptkontoComboBox).bind(r -> getHauptkonto(r, 0), (r, hk) -> setHauptkonto(r, 0, hk));
        binder.forField(unterkontoComboBox).bind(r -> getUnterkonto(r, 0), (r, uk) -> setUnterkonto(r, 0, uk));
        binder.forField(projektComboBox).bind(r -> getProjekt(r, 0), (r, p) -> setProjekt(r, 0, p));
        binder.forField(buchungstextField).bind(r -> getBuchungstext(r, 0), (r, bt) -> setBuchungstext(r, 0, bt));

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.addClassName("spaced-form");
        add(formLayout);

        formLayout.addFormItem(datum, "Datum");
        formLayout.addFormItem(kontonummer, "Kontonummer");
        formLayout.addFormItem(name, "Name");
        formLayout.addFormItem(verwendungszweck, "Verwendungszweck");
        formLayout.addFormItem(betrag, "Betrag (Konto)");
        formLayout.addFormItem(buchungstext, "Buchungstext");

        betragLayout.add(betragField);
        formLayout.addFormItem(betragLayout, "Betrag (Optigem)");
        hauptkontoLayout.add(hauptkontoComboBox);
        formLayout.addFormItem(hauptkontoLayout, "Hauptkonto");
        unterkontoLayout.add(unterkontoComboBox);
        formLayout.addFormItem(unterkontoLayout, "Unterkonto");
        projektLayout.add(projektComboBox);
        formLayout.addFormItem(projektLayout, "Projekt");
        buchungstextLayout.add(buchungstextField);
        formLayout.addFormItem(buchungstextLayout, "Buchungstext");

        saveButton = new Button("Speichern & Schließen", e -> {
            try {
                binder.writeBean(rr);
                rr.fillGeneralData();
                updateTableRow.run();
                close();
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button addAccountToPerson = new Button("Konto \uD83E\uDC52 Person", event -> {
            if (tablePersons == null) {
                MessageDialog.show("Fehler", "keine Personen-Tabelle konfiguriert");
                return;
            }

            if (tableOptigemAccounts == null) {
                MessageDialog.show("Fehler", "keine Konto-Tabelle konfiguriert");
                return;
            }

            if (hauptkontoComboBox.getValue() == null) {
                MessageDialog.show("Fehler", "kein Hauptkonto ausgewählt");
                return;
            }

            if (hauptkontoComboBox.getValue().getId() != Integer.parseInt(accountsHkForPersons)) {
                MessageDialog.show("Fehler", "nicht das personenbezogene Hauptkonto ausgewählt");
                return;
            }

            if (unterkontoComboBox.getValue() == null) {
                MessageDialog.show("Fehler", "kein Unterkonto ausgewählt");
                return;
            }

            Table persons = persistenceService.getTable(tablePersons);
            String iban = rr.getInput().getKontoNummer();
            if (persons.contains(personsColumnIban, iban)) {
                TableRow person = persons.where(personsColumnIban, iban);
                MessageDialog.show("Hinweis", "Kontonummer bereits vorhanden bei " + person.get(personsColumnVorname)
                    + " " + person.get(personsColumnNachname) + " mit Nr. " + person.get(personsColumnNr));
                return;
            }

            TableRow personForUk = persons.where(personsColumnNr, String.valueOf(unterkontoComboBox.getValue().getId()));

            if (unterkontoComboBox.getValue() == null) {
                MessageDialog.show("Fehler", "keine Person zum Unterkonto " + unterkontoComboBox.getValue().getId() + " gefunden");
                return;
            }

            if (StringUtils.isBlank(personForUk.get(personsColumnIban))) {
                personForUk.put(personsColumnIban, iban);
            } else {
                // we already have an account number, so add a new entry:
                persons.add(new TableRow()
                    .with(personsColumnNr, personForUk.get(personsColumnNr))
                    .with(personsColumnIban, iban)
                    .with(personsColumnVorname, personForUk.get(personsColumnVorname))
                    .with(personsColumnNachname, personForUk.get(personsColumnNachname)));
                persons.sortBy(personsColumnNachname, personsColumnVorname, personsColumnNr, personsColumnIban);
            }

            persistenceService.write(persons);

            MessageDialog.show("Erfolg", "Kontonummer zu Person " + personForUk.get(personsColumnVorname) + " "
                + personForUk.get(personsColumnNachname) + " (Nr. " + personForUk.get(personsColumnNr) + ") hinzugefügt");
        });
        addAccountToPerson.setTooltipText("IBAN der Person hinzufügen, die zum gewählten Unterkonto gehört");
        Button addPerson = new Button("Person hinzufügen", event -> {
            if (tablePersons == null) {
                MessageDialog.show("Fehler", "keine Personen-Tabelle konfiguriert");
                return;
            }

            if (tableOptigemAccounts == null) {
                MessageDialog.show("Fehler", "keine Konto-Tabelle konfiguriert");
                return;
            }

            Table persons = persistenceService.getTable(tablePersons);
            String iban = rr.getInput().getKontoNummer();
            if (persons.contains(personsColumnIban, iban)) {
                TableRow person = persons.where(personsColumnIban, iban);
                MessageDialog.show("Hinweis", "Person bereits vorhanden mit Nr. " + person.get(personsColumnNr) + ": "
                    + person.get(personsColumnVorname) + " " + person.get(personsColumnNachname));
                return;
            }

            int nextPersonNummer = persons.max(personsColumnNr) + 1;
            String vorname;
            String nachname;
            if (rr.getInput().getName().contains(",")) {
                String[] nameParts = rr.getInput().getName().split(",", 2);
                nachname = nameParts[0].trim();
                vorname = nameParts[1].trim();
            } else if (rr.getInput().getName().contains(" ")) {
                String[] nameParts = rr.getInput().getName().split(" ", 2);
                vorname = nameParts[0].trim();
                nachname = nameParts[1].trim();
            } else {
                vorname = "";
                nachname = rr.getInput().getName().trim();
            }

            TableRow lastExistingPersonAccount = tableOptigemAccounts.getRows().stream()
                .filter(r -> Objects.equals(r.get(accountsColumnHk), accountsHkForPersons))
                .max(Comparator.comparing(tr -> tr.get(accountsColumnUk)))
                .orElse(null);
            if (lastExistingPersonAccount == null) {
                MessageDialog.show("Fehler", "kein personenbezogenes Unterkonto zu HK " + accountsHkForPersons + " gefunden");
                return;
            }

            int nextUnterkontoNummer = Integer.parseInt(lastExistingPersonAccount.get(accountsColumnUk)) + 1;
            if (nextPersonNummer < nextUnterkontoNummer) {
                nextPersonNummer = nextUnterkontoNummer;
            } else if (nextPersonNummer > nextUnterkontoNummer) {
                nextUnterkontoNummer = nextPersonNummer;
            }
            TableRow newPersonAccount = lastExistingPersonAccount.copy()
                .with(accountsColumnUk, String.valueOf(nextUnterkontoNummer))
                .with(accountsColumnBez, vorname + " " + nachname);
            tableOptigemAccounts.add(newPersonAccount);
            tableOptigemAccounts.sortBy(accountsColumnHk, accountsColumnUk);

            persons.add(new TableRow()
                .with(personsColumnNr, String.valueOf(nextPersonNummer))
                .with(personsColumnIban, iban)
                .with(personsColumnVorname, vorname)
                .with(personsColumnNachname, nachname));
            persons.sortBy(personsColumnNachname, personsColumnVorname);

            persistenceService.write(tableOptigemAccounts);
            persistenceService.write(persons);

            MessageDialog.show("Erfolg", "Person und Unterkonto hinzugefügt: " + vorname + " " + nachname + "\n\nNummer: " + nextPersonNummer);
        });
        addPerson.setTooltipText("Person und IBAN in Tabelle hinzufügen");
        Button addBuchung = new Button("Buchung hinzufügen", event -> {
            rr.getResult().add(new Buchung(null, null, null, null));
            int index = rr.getResult().size() - 1;

            addFieldsForIndex(index);
        });
        addBuchung.setTooltipText("weitere Buchung anfügen");
        Button removeBuchung = new Button("Buchung löschen", e -> {
            if (rr.getResult().size() > 1) {
                AbstractField<?, ?> lastBetrag = lastChildField(betragLayout);
                binder.removeBinding(lastBetrag);
                betragLayout.remove(lastBetrag);
                AbstractField<?, ?> lastHauptkonto = lastChildField(hauptkontoLayout);
                binder.removeBinding(lastHauptkonto);
                hauptkontoLayout.remove(lastHauptkonto);
                AbstractField<?, ?> lastUnterkonto = lastChildField(unterkontoLayout);
                binder.removeBinding(lastUnterkonto);
                unterkontoLayout.remove(lastUnterkonto);
                AbstractField<?, ?> lastProjekt = lastChildField(projektLayout);
                binder.removeBinding(lastProjekt);
                projektLayout.remove(lastProjekt);
                AbstractField<?, ?> lastBuchungstext = lastChildField(buchungstextLayout);
                binder.removeBinding(lastBuchungstext);
                buchungstextLayout.remove(lastBuchungstext);
                rr.getResult().remove(rr.getResult().size() - 1);
            }
        });
        removeBuchung.setTooltipText("letzte Buchung entfernen");
        Button deleteButton = new Button("Löschen & Schließen", e -> {
            rr.clearBuchungen();
            updateTableRow.run();
            close();
        });
        deleteButton.setTabIndex(-1);
        HorizontalLayout buttons = new HorizontalLayout(FlexComponent.JustifyContentMode.BETWEEN,
            saveButton, addAccountToPerson, addPerson, addBuchung, removeBuchung, deleteButton);
        add(buttons);

        final Holder<Boolean> initializing = new Holder<>(true);

        hauptkontoComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && tableOptigemAccounts != null) {
                unterkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                    .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnUk) != null
                        && r.get(accountsColumnHk).equals(String.valueOf(e.getValue().getId())))
                    .map(r -> new IdAndName(Integer.parseInt(r.get(accountsColumnUk)), r.get(accountsColumnBez)))
                    .toList()));
                setCalculatedComboboxDropdownWidth(unterkontoComboBox);
            } else {
                unterkontoComboBox.setItems(Collections.emptyList());
            }

            List<IdAndName> unterkonten = new ArrayList<>(((ListDataProvider<IdAndName>) unterkontoComboBox.getDataProvider()).getItems());
            if (unterkonten.size() == 1) {
                unterkontoComboBox.setValue(unterkonten.get(0));
            } else if (unterkonten.size() > 1) {
                automaticFocusChangeAllowed.setValue(false);
                unterkontoComboBox.setValue(unterkonten.get(0));
                automaticFocusChangeAllowed.setValue(true);
            }
            if (!initializing.getValue() && unterkonten.size() > 1) {
                e.getSource().setOpened(false);
                if (automaticFocusChangeAllowed.getValue()) {
                    unterkontoComboBox.focus();
                }
            }

            if (hauptkontoValueChangeTask != null) {
                hauptkontoValueChangeTask.run();
                hauptkontoValueChangeTask = null;
            }
        });
        unterkontoComboBox.addValueChangeListener(e -> {
            List<IdAndName> projekte = new ArrayList<>(((ListDataProvider<IdAndName>) projektComboBox.getDataProvider()).getItems());
            if (projekte.size() == 1) {
                projektComboBox.setValue(projekte.get(0));
            }
            if (!initializing.getValue()) {
                if (projektComboBox.isEmpty()) {
                    projektComboBox.setValue(projekte.get(0));
                }
                unterkontoComboBox.setOpened(false);
                if (automaticFocusChangeAllowed.getValue()) {
                    projektComboBox.focus();
                }
            }
        });
        projektComboBox.addValueChangeListener(e -> {
            if (!e.getHasValue().isEmpty() && !initializing.getValue()) {
                projektComboBox.setOpened(false);
                if (automaticFocusChangeAllowed.getValue()) {
                    buchungstextField.focus();
                }
            }
        });
        buchungstextField.addValueChangeListener(e -> {
            if (tooLong(e.getOldValue()) && !tooLong(e.getValue())) {
                buchungstextField.setHelperComponent(null);
            } else if (!tooLong(e.getOldValue()) && tooLong(e.getValue())) {
                buchungstextField.setHelperComponent(new Span("Text zu lang - wird in Optigem gekürzt."));
                buchungstextField.getHelperComponent().setClassName("bold-orange");
            }
        });

        for (int index = 1; index < rr.getResult().size(); index++) {
            addFieldsForIndex(index);
        }

        binder.readBean(rr);

        // AFTER reading the bean:
        if (!rr.hasBuchung()) {
            rr.getResult().add(new Buchung(null, null, null, null));
            betragField.setValue(CURRENCY_FORMAT.format(rr.getInput().getBetrag()));
        }

        if (buchungstextField.isEmpty()) {
            buchungstextField.setValue(StringUtils.isNotBlank(rr.getInput().getVerwendungszweckClean())
                ? rr.getInput().getVerwendungszweckClean().trim() + " - " + rr.getInput().getName()
                : rr.getInput().getName());
        }

        initializing.setValue(false);

        Shortcuts.addShortcutListener(betragField, hauptkontoComboBox::focus, Key.ENTER)
            .listenOn(betragField);
        Shortcuts.addShortcutListener(hauptkontoComboBox, () -> {
                hauptkontoValueChangeTask = () -> {
                    boolean automaticFocusChangeWasAllowed = automaticFocusChangeAllowed.getValue();
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(false);
                    }

                    hauptkontoComboBox.setOpened(false);
                    IdAndName selectedUnterkonto = unterkontoComboBox.getValue();
                    unterkontoComboBox.setValue(((ListDataProvider<IdAndName>) unterkontoComboBox.getDataProvider()).getItems().stream()
                        .findFirst().orElse(null));
                    unterkontoComboBox.setValue(selectedUnterkonto);
                    IdAndName selectedProjekt = projektComboBox.getValue();
                    projektComboBox.setValue(((ListDataProvider<IdAndName>) projektComboBox.getDataProvider()).getItems().stream()
                        .findFirst().orElse(null));
                    projektComboBox.setValue(selectedProjekt);
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(true);
                    }
                };
                applyFilter(hauptkontoComboBox);
            }, Key.ENTER)
            .listenOn(hauptkontoComboBox);
        Shortcuts.addShortcutListener(unterkontoComboBox, () -> applyFilter(unterkontoComboBox), Key.ENTER)
            .listenOn(unterkontoComboBox);
        Shortcuts.addShortcutListener(projektComboBox, () -> applyFilter(projektComboBox), Key.ENTER)
            .listenOn(projektComboBox);
        Shortcuts.addShortcutListener(buchungstextField, saveButton::focus, Key.ENTER)
            .listenOn(buchungstextField);

        openHauptkontoComboBox.setValue(false);
        hauptkontoComboBox.focus();
    }
    private void addFieldsForIndex(final int index) {
        TextField extraBetragField = new TextField();
        extraBetragField.setId("betragField" + index);
        extraBetragField.setWidthFull();
        betragLayout.add(extraBetragField);
        binder.forField(extraBetragField).bind(r -> getBetrag(r, index), (r, b) -> setBetrag(r, index, b));

        ComboBox<IdAndName> extraHauptkontoComboBox = new ComboBox<>();
        extraHauptkontoComboBox.setId("hauptkontoComboBox" + index);
        extraHauptkontoComboBox.setAutoOpen(true);
        extraHauptkontoComboBox.setWidthFull();
        extraHauptkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        hauptkontoLayout.add(extraHauptkontoComboBox);
        binder.forField(extraHauptkontoComboBox).bind(r -> getHauptkonto(r, index), (r, hk) -> setHauptkonto(r, index, hk));

        ComboBox<IdAndName> extraUnterkontoComboBox = new ComboBox<>();
        extraUnterkontoComboBox.setId("unterkontoComboBox" + index);
        extraUnterkontoComboBox.setAutoOpen(true);
        extraUnterkontoComboBox.setWidthFull();
        extraUnterkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        unterkontoLayout.add(extraUnterkontoComboBox);
        binder.forField(extraUnterkontoComboBox).bind(r -> getUnterkonto(r, index), (r, uk) -> setUnterkonto(r, index, uk));

        ComboBox<IdAndName> extraProjektComboBox = new ComboBox<>();
        extraProjektComboBox.setId("projektComboBox" + index);
        extraProjektComboBox.setAutoOpen(true);
        extraProjektComboBox.setWidthFull();
        extraProjektComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        projektLayout.add(extraProjektComboBox);
        binder.forField(extraProjektComboBox).bind(r -> getProjekt(r, index), (r, p) -> setProjekt(r, index, p));

        TextField extraBuchungstextField = new TextField();
        extraBuchungstextField.setId("buchungstextField" + index);
        extraBuchungstextField.setWidthFull();
        buchungstextLayout.add(extraBuchungstextField);
        binder.forField(extraBuchungstextField).bind(r -> getBuchungstext(r, index), (r, bt) -> setBuchungstext(r, index, bt));
        extraBuchungstextField.setValue(rr.getInput().getVerwendungszweckClean());

        if (tableOptigemAccounts != null) {
            extraHauptkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnUk) != null
                    && r.get(accountsColumnUk).equals("0"))
                .map(r -> new IdAndName(Integer.parseInt(r.get(accountsColumnHk)), r.get(accountsColumnBez)))
                .toList()));
            setCalculatedComboboxDropdownWidth(extraHauptkontoComboBox);
        }

        if (tableOptigemProjects != null) {
            extraProjektComboBox.setItems(new ListDataProvider<>(tableOptigemProjects.getRows().stream()
                .filter(r -> r.get(projectsColumnNr) != null && r.get(projectsColumnBez) != null)
                .map(r -> new IdAndName(Integer.parseInt(r.get(projectsColumnNr)), r.get(projectsColumnBez)))
                .toList()));
            setCalculatedComboboxDropdownWidth(extraProjektComboBox);
        }

        extraHauptkontoComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && tableOptigemAccounts != null) {
                extraUnterkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                    .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnUk) != null
                        && r.get(accountsColumnHk).equals(String.valueOf(e.getValue().getId())))
                    .map(r -> new IdAndName(Integer.parseInt(r.get(accountsColumnUk)), r.get(accountsColumnBez)))
                    .toList()));
                setCalculatedComboboxDropdownWidth(extraUnterkontoComboBox);
            } else {
                extraUnterkontoComboBox.setItems(Collections.emptyList());
            }

            List<IdAndName> unterkonten = new ArrayList<>(((ListDataProvider<IdAndName>) extraUnterkontoComboBox.getDataProvider()).getItems());
            if (unterkonten.size() == 1) {
                extraUnterkontoComboBox.setValue(unterkonten.get(0));
            } else if (unterkonten.size() > 1) {
                automaticFocusChangeAllowed.setValue(false);
                extraUnterkontoComboBox.setValue(unterkonten.get(0));
                automaticFocusChangeAllowed.setValue(true);
            }
            if (unterkonten.size() > 1) {
                e.getSource().setOpened(false);
                if (automaticFocusChangeAllowed.getValue()) {
                    extraUnterkontoComboBox.focus();
                }
            }

            if (hauptkontoValueChangeTask != null) {
                hauptkontoValueChangeTask.run();
                hauptkontoValueChangeTask = null;
            }
        });
        extraUnterkontoComboBox.addValueChangeListener(e -> {
            List<IdAndName> projekte = new ArrayList<>(((ListDataProvider<IdAndName>) extraProjektComboBox.getDataProvider()).getItems());
            if (projekte.size() == 1) {
                extraProjektComboBox.setValue(projekte.get(0));
            }
            if (extraProjektComboBox.isEmpty()) {
                extraProjektComboBox.setValue(projekte.get(0));
            }
            extraUnterkontoComboBox.setOpened(false);
            if (automaticFocusChangeAllowed.getValue()) {
                extraProjektComboBox.focus();
            }
        });
        extraProjektComboBox.addValueChangeListener(e -> {
            if (!e.getHasValue().isEmpty()) {
                extraProjektComboBox.setOpened(false);
                if (automaticFocusChangeAllowed.getValue()) {
                    extraBuchungstextField.focus();
                }
            }
        });

        Shortcuts.addShortcutListener(extraBetragField, extraHauptkontoComboBox::focus, Key.ENTER)
            .listenOn(extraBetragField);
        Shortcuts.addShortcutListener(extraHauptkontoComboBox, () -> {
                hauptkontoValueChangeTask = () -> {
                    boolean automaticFocusChangeWasAllowed = automaticFocusChangeAllowed.getValue();
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(false);
                    }

                    extraHauptkontoComboBox.setOpened(false);
                    IdAndName selectedUnterkonto = extraUnterkontoComboBox.getValue();
                    extraUnterkontoComboBox.setValue(((ListDataProvider<IdAndName>) extraUnterkontoComboBox.getDataProvider()).getItems().stream()
                        .findFirst().orElse(null));
                    extraUnterkontoComboBox.setValue(selectedUnterkonto);
                    IdAndName selectedProjekt = extraProjektComboBox.getValue();
                    extraProjektComboBox.setValue(((ListDataProvider<IdAndName>) extraProjektComboBox.getDataProvider()).getItems().stream()
                        .findFirst().orElse(null));
                    extraProjektComboBox.setValue(selectedProjekt);
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(true);
                    }
                };
                applyFilter(extraHauptkontoComboBox);
            }, Key.ENTER)
            .listenOn(extraHauptkontoComboBox);
        Shortcuts.addShortcutListener(extraUnterkontoComboBox, () -> applyFilter(extraUnterkontoComboBox), Key.ENTER)
            .listenOn(extraUnterkontoComboBox);
        Shortcuts.addShortcutListener(extraProjektComboBox, () -> applyFilter(extraProjektComboBox), Key.ENTER)
            .listenOn(extraProjektComboBox);
        Shortcuts.addShortcutListener(extraBuchungstextField, saveButton::focus, Key.ENTER)
            .listenOn(extraBuchungstextField);
    }

    private static AbstractField<?, ?> lastChildField(Component container) {
        List<? extends AbstractField<?, ?>> components = container.getChildren()
            .filter(c -> c instanceof AbstractField)
            .map(c -> (AbstractField<?, ?>) c)
            .toList();
        return components.isEmpty() ? null : components.get(components.size() - 1);
    }

    private static boolean tooLong(String str) {
        return !StringUtils.isEmpty(str) && str.length() > 52;
    }

    @SuppressWarnings("unchecked")
    private void applyFilter(final ComboBox<IdAndName> current) {
        String currentId = current.getId().orElseThrow();
        PendingJavaScriptResult scriptResult =
            UI.getCurrent().getPage().executeJs("return document.getElementById('" + currentId + "').getElementsByTagName('input')[0].value;");
        scriptResult.then(String.class, filter -> {
            String idFilter = filter == null
                ? null
                : EVERYTHING_AFTER_SPACE.matcher(filter).replaceAll("");
            ((ListDataProvider<IdAndName>) current.getDataProvider()).getItems().stream()
                .filter(e -> String.valueOf(e.getId()).equals(idFilter))
                .findAny()
                .ifPresentOrElse(e -> {
                    // filter for ID
                    log.debug("{} idFilter={} => element={}", currentId, idFilter, e);
                    boolean automaticFocusChangeWasAllowed = automaticFocusChangeAllowed.getValue();
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(false);
                    }
                    current.setValue(null);
                    if (automaticFocusChangeWasAllowed) {
                        automaticFocusChangeAllowed.setValue(true);
                    }
                    current.setValue(e);
                }, () -> {
                    // filter for entry name
                    String filterLowercase = filter == null ? null : filter.toLowerCase();
                    List<IdAndName> filtered = ((ListDataProvider<IdAndName>) current.getDataProvider()).getItems().stream()
                        .filter(e -> filterLowercase != null && e.getName().toLowerCase().contains(filterLowercase))
                        .toList();
                    if (filtered.size() == 1) {
                        IdAndName found = filtered.get(0);
                        log.debug("{} filter={} => element={}", currentId, filter, found);
                        boolean automaticFocusChangeWasAllowed = automaticFocusChangeAllowed.getValue();
                        if (automaticFocusChangeWasAllowed) {
                            automaticFocusChangeAllowed.setValue(false);
                        }
                        current.setValue(null);
                        if (automaticFocusChangeWasAllowed) {
                            automaticFocusChangeAllowed.setValue(true);
                        }
                        current.setValue(found);
                    } else {
                        log.debug("{} filter={} idFilter={} => no element found", currentId, filter, idFilter);
                    }
                });
        }, error -> log.debug("{} error while getting filter text: {}", currentId, error));
        try {
            scriptResult.toCompletableFuture().get();
        } catch (Exception ignored) {
            // do nothing
        }
    }

    private static void setCalculatedComboboxDropdownWidth(ComboBox<?> cmb) {
        if (cmb.getDataProvider() instanceof ListDataProvider<?>) {
            ((ListDataProvider<?>) cmb.getDataProvider()).getItems().stream()
                .mapToInt(o -> o.toString().length())
                .max()
                .ifPresent(width ->
                    cmb.getElement().getStyle().set("--vaadin-combo-box-overlay-width", (width + 5) + "em")
                );
        }
    }

    private static String getBetrag(RuleResult rr, int index) {
        if (rr.getResult().size() <= index) {
            return null;
        }
        return rr.getResult().get(index).getBetrag() == null
            ? null
            : CURRENCY_FORMAT.format(rr.getResult().get(index).getBetrag());

    }

    private static void setBetrag(RuleResult rr, int index, String betrag) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(null, null, null, null));
        }
        try {
            Number parsed = CURRENCY_FORMAT.parse(betrag);
            rr.getResult().get(index).setBetrag(new BigDecimal(parsed.toString()));
        } catch (ParseException e) {
            log.warn("could not parse number: {}", betrag);
        }
    }

    private IdAndName getHauptkonto(RuleResult rr, int index) {
        if (rr.getResult().size() <= index) {
            return null;
        }
        String name = getKontoName(rr.getResult().get(index).getHauptkonto(), 0);
        return new IdAndName(rr.getResult().get(index).getHauptkonto(), name);
    }

    private static void setHauptkonto(RuleResult rr, int index, IdAndName hk) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(hk.getId(), null, null, null));
        }
        rr.getResult().get(index).setHauptkonto(hk.getId());
    }

    private IdAndName getUnterkonto(RuleResult rr, int index) {
        if (rr.getResult().size() <= index) {
            return null;
        }
        String name = getKontoName(rr.getResult().get(index).getHauptkonto(), rr.getResult().get(index).getUnterkonto());
        return new IdAndName(rr.getResult().get(index).getUnterkonto(), name);
    }

    private static void setUnterkonto(RuleResult rr, int index, IdAndName uk) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(null, uk.getId(), null, null));
        }
        rr.getResult().get(index).setUnterkonto(uk == null ? 0 : uk.getId());
    }

    private IdAndName getProjekt(RuleResult rr, int index) {
        if (rr.getResult().size() <= index) {
            return null;
        }
        String name = getProjektName(rr.getResult().get(index).getProjekt());
        return new IdAndName(rr.getResult().get(index).getProjekt(), name);
    }

    private static void setProjekt(RuleResult rr, int index, IdAndName proj) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(null, null, proj.getId(), null));
        }
        rr.getResult().get(index).setProjekt(proj == null ? 0 : proj.getId());
    }

    private static String getBuchungstext(RuleResult rr, int index) {
        if (rr.getResult().size() <= index) {
            return null;
        }
        return rr.getResult().get(index).getBuchungstext();
    }

    private static void setBuchungstext(RuleResult rr, int index, String buchungstext) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(null, null, null, buchungstext));
        }
        rr.getResult().get(index).setBuchungstext(buchungstext);
    }

    private String getKontoName(int hk, int uk) {
        if (tableOptigemAccounts == null) {
            return null;
        }
        return tableOptigemAccounts.getRows().stream()
            .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnHk).equals(String.valueOf(hk))
                && r.get(accountsColumnUk) != null && r.get(accountsColumnUk).equals(String.valueOf(uk)))
            .map(r -> r.get(accountsColumnBez))
            .findFirst()
            .orElse("?");
    }

    private String getProjektName(int nr) {
        if (tableOptigemProjects == null) {
            return null;
        }
        return tableOptigemProjects.getRows().stream()
            .filter(r -> r.get("Nr") != null && r.get("Nr").equals(String.valueOf(nr)))
            .map(r -> r.get("Name"))
            .findFirst()
            .orElse(null);
    }
}
