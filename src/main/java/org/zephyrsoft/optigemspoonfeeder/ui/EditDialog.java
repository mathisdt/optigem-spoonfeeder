package org.zephyrsoft.optigemspoonfeeder.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.IdAndName;
import org.zephyrsoft.optigemspoonfeeder.model.PaypalBooking;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;
import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;
import org.zephyrsoft.optigemspoonfeeder.service.PersonService;

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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.Command;

import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Slf4j
@SuppressWarnings({"NonSerializableFieldInSerializableClass", "serial"})
final class EditDialog extends Dialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(Locale.GERMAN);
    private static final Pattern EVERYTHING_AFTER_SPACE = Pattern.compile(" .*$");
    private static final String BUCHUNGSTEXT_PATTERN = "^.{0,60}$";

    private static final String HAUPTKONTO_COMBO_BOX_ID = "hauptkontoComboBox";
    private static final String UNTERKONTO_COMBO_BOX_ID = "unterkontoComboBox";
    private static final String PROJEKT_COMBO_BOX_ID = "projektComboBox";

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
    private final Binder<RuleResult> binder;
    private final HorizontalLayout betragLayout;
    private final HorizontalLayout hauptkontoLayout;
    private final HorizontalLayout unterkontoLayout;
    private final HorizontalLayout projektLayout;
    private final HorizontalLayout buchungstextLayout;
    private final Button saveButton;
    private final List<TextField> betragFields = new ArrayList<>();
    private final List<ComboBox<IdAndName>> hauptkontoFields = new ArrayList<>();
    private final List<ComboBox<IdAndName>> unterkontoFields = new ArrayList<>();
    private final List<ComboBox<IdAndName>> projektFields = new ArrayList<>();
    private final List<TextField> buchungstextFields = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public EditDialog(OptigemSpoonfeederProperties.AccountProperties accountProperties, Table tableOptigemAccounts, String accountsColumnHk,
        String accountsColumnUk, String accountsColumnBez, Table tableOptigemProjects, String projectsColumnNr, String projectsColumnBez,
        String tablePersons, String personsColumnNr, String personsColumnIban, String personsColumnVorname, String personsColumnNachname,
        String accountsHkForPersons,
        RuleResult rr, Runnable updateTableRow, PersistenceService persistenceService, PersonService personService,
        List<PaypalBooking> paypalBookings) {
        this.tableOptigemAccounts = tableOptigemAccounts;
        this.accountsColumnHk = accountsColumnHk;
        this.accountsColumnUk = accountsColumnUk;
        this.accountsColumnBez = accountsColumnBez;
        this.tableOptigemProjects = tableOptigemProjects;
        this.projectsColumnNr = projectsColumnNr;
        this.projectsColumnBez = projectsColumnBez;
        this.rr = rr;

        if (accountProperties == null) {
            throw new IllegalArgumentException("kein Bankkonto angegeben");
        }

        setWidth("65%");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        if (rr.getResult().isEmpty()) {
            // one booking should always be present
            Buchung firstBooking = new Buchung(-1, -1, -1, null);
            firstBooking.setBetrag(rr.getInput().getBetrag());
            rr.getResult().add(firstBooking);
        }

        setHeaderTitle("Buchung bearbeiten");
        Button closeButton = new Button(new Icon("lumo", "cross"),
            (e) -> close());
        closeButton.setTooltipText("Schließen ohne Speichern");
        closeButton.setTabIndex(-1);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ComboBox<PaypalBooking> paypalDonation = null;
        if (paypalBookings != null) {
            paypalDonation = new ComboBox<>();
            paypalDonation.setPlaceholder("Paypal-Spende...");
            paypalDonation.setItems(paypalBookings.stream().filter(PaypalBooking::isIncomingPayment).toList());
            paypalDonation.setItemLabelGenerator(pb -> String.format(Locale.GERMAN,
                "%1.2f %2s (%3s %4s, %5$td.%5$tm.)",
                pb.getNetAmount(), pb.getCurrency(), pb.getFirstName(), pb.getLastName(), pb.getDate()));
            getHeader().add(paypalDonation, closeButton);
        } else {
            getHeader().add(closeButton);
        }

        Span datum = new Span(DATE_FORMAT.format(rr.getInput().getValutaDatum()));
        Span kontonummer = new Span(rr.getInput().getKontoNummer());
        Span name = new Span(rr.getInput().getName());
        Span verwendungszweck = new Span(rr.getInput().getVerwendungszweckCleanOneline());
        Span betrag = new Span(CURRENCY_FORMAT.format(rr.getInput().getBetragMitVorzeichen()) + " €");
        betrag.addClassName(rr.getInput().isCredit() ? "green" : "red");
        Span buchungstext = new Span(rr.getInput().getBuchungstext());

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

        TextField betragField = new TextField();
        betragFields.add(betragField);
        betragField.setId("betragField");
        betragField.setWidthFull();

        ComboBox<IdAndName> hauptkontoComboBox = new ComboBox<>();
        hauptkontoFields.add(hauptkontoComboBox);
        hauptkontoComboBox.setId(HAUPTKONTO_COMBO_BOX_ID);
        hauptkontoComboBox.setAutoOpen(true);
        hauptkontoComboBox.setWidthFull();
        hauptkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        hauptkontoComboBox.getElement().addEventListener("click", e -> selectAllText(hauptkontoComboBox));
        hauptkontoComboBox.getElement().addEventListener("focus", e -> selectAllText(hauptkontoComboBox));

        ComboBox<IdAndName> unterkontoComboBox = new ComboBox<>();
        unterkontoFields.add(unterkontoComboBox);
        unterkontoComboBox.setId(UNTERKONTO_COMBO_BOX_ID);
        unterkontoComboBox.setAutoOpen(true);
        unterkontoComboBox.setWidthFull();
        unterkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        unterkontoComboBox.getElement().addEventListener("click", e -> selectAllText(unterkontoComboBox));
        unterkontoComboBox.getElement().addEventListener("focus", e -> selectAllText(unterkontoComboBox));

        ComboBox<IdAndName> projektComboBox = new ComboBox<>();
        projektFields.add(projektComboBox);
        projektComboBox.setId(PROJEKT_COMBO_BOX_ID);
        projektComboBox.setAutoOpen(true);
        projektComboBox.setWidthFull();
        projektComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        projektComboBox.getElement().addEventListener("click", e -> selectAllText(projektComboBox));
        projektComboBox.getElement().addEventListener("focus", e -> selectAllText(projektComboBox));

        TextField buchungstextField = new TextField();
        buchungstextFields.add(buchungstextField);
        buchungstextField.setId("buchungstextField");
        buchungstextField.setWidthFull();

        if (tableOptigemAccounts != null) {
            hauptkontoComboBox.setItems(IdAndName::matchesFilter, new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
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
        } else {
            projektComboBox.setItems(new IdAndName(0, "allgemein"));
        }
        setCalculatedComboboxDropdownWidth(projektComboBox);

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

        if (paypalDonation != null) {
            paypalDonation.addValueChangeListener(e -> {
                PaypalBooking paypalBooking = e.getValue();
                if (paypalBooking != null) {
                    betragFields.getLast().setValue(CURRENCY_FORMAT.format(paypalBooking.getNetAmount()));
                    String hauptkontoName = getKontoName(8010, 0);
                    IdAndName unterkonto = getDonationUnterkontoByName(paypalBooking.getFirstName(), paypalBooking.getLastName());
                    String projektName = getProjektName(0);
                    hauptkontoFields.getLast().setValue(new IdAndName(8010, hauptkontoName));
                    unterkontoFields.getLast().setValue(unterkonto);
                    projektFields.getLast().setValue(new IdAndName(0, projektName));
                    buchungstextFields.getLast().setValue("Spende via Paypal - "
                        + (isNotBlank(paypalBooking.getDescription()) ? paypalBooking.getDescription() + " - " : "")
                        + trimToEmpty(paypalBooking.getFirstName()) + " " + trimToEmpty(paypalBooking.getLastName()) + ", "
                        + trimToEmpty(paypalBooking.getStreet()) + ", "
                        + trimToEmpty(paypalBooking.getZip()) + " " + trimToEmpty(paypalBooking.getCity()));
                    e.getSource().setValue(null);
                }
            });
        }

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

            PersonDialog personDialog = new PersonDialog(accountProperties, tableOptigemAccounts, personService, nextPersonNummer,
                vorname, nachname, iban,
                newPersonNumber -> {
                    if (newPersonNumber != null) {
                        // update data in all columns
                        for (int i = 0; i < hauptkontoFields.size(); i++) {
                            fillUnterkontoComboBox(unterkontoFields.get(i), hauptkontoFields.get(i));
                            setCalculatedComboboxDropdownWidth(unterkontoFields.get(i));
                        }
                        // select newly created entry in the rightmost column
                        unterkontoFields.getLast().getListDataView().getItems()
                            .filter(element -> element.getId() == newPersonNumber)
                            .findAny()
                            .ifPresent(unterkontoFields.getLast()::setValue);
                    }
                });
            personDialog.open();
        });
        addPerson.setTooltipText("Person und IBAN in Tabelle hinzufügen");
        Button addBuchung = new Button("Buchung hinzufügen", event -> {
            Buchung buchung = new Buchung(null, null, null, null);
            rr.getResult().add(buchung);
            BigDecimal usedAmount = betragFields.stream()
                .map(tf -> {
                    try {
                        return new BigDecimal(CURRENCY_FORMAT.parse(tf.getValue()).toString());
                    } catch (ParseException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal restAmount = rr.getInput().getBetrag().subtract(usedAmount);
            if (restAmount.signum() == 1) {
                buchung.setBetrag(restAmount);
            }
            int index = rr.getResult().size() - 1;

            addFieldsForIndex(index, buchung);
        });
        addBuchung.setTooltipText("weitere Buchung anfügen");
        Button removeBuchung = new Button("Buchung löschen", e -> {
            if (rr.getResult().size() > 1) {
                AbstractField<?, ?> lastBetrag = lastChildField(betragLayout);
                binder.removeBinding(lastBetrag);
                betragLayout.remove(lastBetrag);
                betragFields.removeLast();
                AbstractField<?, ?> lastHauptkonto = lastChildField(hauptkontoLayout);
                binder.removeBinding(lastHauptkonto);
                hauptkontoLayout.remove(lastHauptkonto);
                hauptkontoFields.removeLast();
                AbstractField<?, ?> lastUnterkonto = lastChildField(unterkontoLayout);
                binder.removeBinding(lastUnterkonto);
                unterkontoLayout.remove(lastUnterkonto);
                unterkontoFields.removeLast();
                AbstractField<?, ?> lastProjekt = lastChildField(projektLayout);
                binder.removeBinding(lastProjekt);
                projektLayout.remove(lastProjekt);
                projektFields.removeLast();
                AbstractField<?, ?> lastBuchungstext = lastChildField(buchungstextLayout);
                binder.removeBinding(lastBuchungstext);
                buchungstextLayout.remove(lastBuchungstext);
                buchungstextFields.removeLast();
                rr.getResult().removeLast();
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

        hauptkontoComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                fillUnterkontoComboBox(unterkontoComboBox, hauptkontoComboBox);
                setCalculatedComboboxDropdownWidth(unterkontoComboBox);
            }
            if (e.isFromClient()) {
                e.getSource().setOpened(false);
                unterkontoComboBox.focus();
                selectAllText(unterkontoComboBox);
            }
        });
        unterkontoComboBox.addValueChangeListener(e -> {
            List<IdAndName> projekte = new ArrayList<>(((ListDataProvider<IdAndName>) projektComboBox.getDataProvider()).getItems());
            if (projekte.size() == 1 || projektComboBox.isEmpty()) {
                projektComboBox.setValue(projekte.getFirst());
            }
            if (e.isFromClient()) {
                unterkontoComboBox.setOpened(false);
                projektComboBox.focus();
                selectAllText(projektComboBox);
            }
        });
        projektComboBox.addValueChangeListener(e -> {
            if (!e.getHasValue().isEmpty() && e.isFromClient()) {
                projektComboBox.setOpened(false);
                buchungstextField.focus();
            }
        });
        buchungstextField.addClassName("buchungstext_field");
        buchungstextField.setPattern(BUCHUNGSTEXT_PATTERN);
        buchungstextField.setI18n(new TextField.TextFieldI18n()
            .setPatternErrorMessage("Text zu lang"));
        buchungstextField.setValueChangeMode(ValueChangeMode.EAGER);

        for (int index = 1; index < rr.getResult().size(); index++) {
            addFieldsForIndex(index, null);
        }

        binder.readBean(rr);

        // AFTER reading the bean:
        if (!rr.hasBuchung()) {
            betragField.setValue(CURRENCY_FORMAT.format(rr.getInput().getBetrag()));
        }

        if (buchungstextField.isEmpty()) {
            buchungstextField.setValue(isNotBlank(rr.getInput().getVerwendungszweckClean())
                ? rr.getInput().getVerwendungszweckClean().trim() + " - " + rr.getInput().getName()
                : rr.getInput().getName());
        }

        Shortcuts.addShortcutListener(betragField, () -> {
                hauptkontoComboBox.focus();
                selectAllText(hauptkontoComboBox);
            }, Key.ENTER)
            .listenOn(betragField);

        hauptkontoComboBox.setAllowCustomValue(true);
        hauptkontoComboBox.addCustomValueSetListener(e -> {
            applyFilter(e.getDetail(), hauptkontoComboBox);
            unterkontoComboBox.focus();
            selectAllText(unterkontoComboBox);
        });

        Shortcuts.addShortcutListener(hauptkontoComboBox, () -> {
            hauptkontoComboBox.blur();
            unterkontoComboBox.focus();
            selectAllText(unterkontoComboBox);
            }, Key.ENTER)
            .listenOn(hauptkontoComboBox);

        unterkontoComboBox.setAllowCustomValue(true);
        unterkontoComboBox.addCustomValueSetListener(e ->
            applyFilter(e.getDetail(), unterkontoComboBox));
        Shortcuts.addShortcutListener(unterkontoComboBox, () -> {
            unterkontoComboBox.blur();
                projektComboBox.focus();
                selectAllText(projektComboBox);
            }, Key.ENTER)
            .listenOn(unterkontoComboBox);
        projektComboBox.setAllowCustomValue(true);
        projektComboBox.addCustomValueSetListener(e ->
            applyFilter(e.getDetail(), projektComboBox));
        Shortcuts.addShortcutListener(projektComboBox, () -> {
            projektComboBox.blur();
            buchungstextField.focus();
            }, Key.ENTER)
            .listenOn(projektComboBox);
        Shortcuts.addShortcutListener(buchungstextField, (Command) saveButton::focus, Key.ENTER)
            .listenOn(buchungstextField);

        hauptkontoComboBox.focus();
        selectAllText(hauptkontoComboBox);
    }

    private static void selectAllText(ComboBox<?> dropdown) {
        if (dropdown.getId().isPresent()) {
            UI.getCurrent().getPage()
                .executeJs("document.getElementById('" + dropdown.getId().get() + "').getElementsByTagName('input')[0].select();");
        }
    }

    private void fillUnterkontoComboBox(final ComboBox<IdAndName> unterkontoComboBox, final ComboBox<IdAndName> hauptComboBox) {
        if (tableOptigemAccounts != null && hauptComboBox.getValue() != null) {
            List<IdAndName> values = tableOptigemAccounts.getRows().stream()
                .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnUk) != null
                    && r.get(accountsColumnHk).equals(String.valueOf(hauptComboBox.getValue().getId())))
                .map(r -> new IdAndName(Integer.parseInt(r.get(accountsColumnUk)), r.get(accountsColumnBez)))
                .toList();
            unterkontoComboBox.setItems(new ListDataProvider<>(values));
            if (!values.isEmpty()) {
                unterkontoComboBox.setValue(values.getFirst());
            }

        } else {
            unterkontoComboBox.setItems(Collections.emptyList());
        }
    }

    @SuppressWarnings("unchecked")
    private void addFieldsForIndex(final int index, Buchung buchung) {
        TextField extraBetragField = new TextField();
        betragFields.add(extraBetragField);
        extraBetragField.setId("betragField" + index);
        extraBetragField.setWidthFull();
        if (buchung != null && buchung.getBetrag() != null) {
            extraBetragField.setValue(CURRENCY_FORMAT.format(buchung.getBetrag()));
        }
        betragLayout.add(extraBetragField);
        binder.forField(extraBetragField).bind(r -> getBetrag(r, index), (r, b) -> setBetrag(r, index, b));

        ComboBox<IdAndName> extraHauptkontoComboBox = new ComboBox<>();
        hauptkontoFields.add(extraHauptkontoComboBox);
        extraHauptkontoComboBox.setId("hauptkontoComboBox" + index);
        extraHauptkontoComboBox.setAutoOpen(true);
        extraHauptkontoComboBox.setWidthFull();
        extraHauptkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        hauptkontoLayout.add(extraHauptkontoComboBox);
        binder.forField(extraHauptkontoComboBox).bind(r -> getHauptkonto(r, index), (r, hk) -> setHauptkonto(r, index, hk));

        ComboBox<IdAndName> extraUnterkontoComboBox = new ComboBox<>();
        unterkontoFields.add(extraUnterkontoComboBox);
        extraUnterkontoComboBox.setId("unterkontoComboBox" + index);
        extraUnterkontoComboBox.setAutoOpen(true);
        extraUnterkontoComboBox.setWidthFull();
        extraUnterkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        unterkontoLayout.add(extraUnterkontoComboBox);
        binder.forField(extraUnterkontoComboBox).bind(r -> getUnterkonto(r, index), (r, uk) -> setUnterkonto(r, index, uk));

        ComboBox<IdAndName> extraProjektComboBox = new ComboBox<>();
        projektFields.add(extraProjektComboBox);
        extraProjektComboBox.setId("projektComboBox" + index);
        extraProjektComboBox.setAutoOpen(true);
        extraProjektComboBox.setWidthFull();
        extraProjektComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        projektLayout.add(extraProjektComboBox);
        binder.forField(extraProjektComboBox).bind(r -> getProjekt(r, index), (r, p) -> setProjekt(r, index, p));

        TextField extraBuchungstextField = new TextField();
        buchungstextFields.add(extraBuchungstextField);
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
        } else {
            extraProjektComboBox.setItems(new IdAndName(0, "allgemein"));
        }

        extraHauptkontoComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                fillUnterkontoComboBox(extraUnterkontoComboBox, extraHauptkontoComboBox);
                setCalculatedComboboxDropdownWidth(extraUnterkontoComboBox);
            }

            if (e.isFromClient()) {
                e.getSource().setOpened(false);
                extraUnterkontoComboBox.focus();
                selectAllText(extraUnterkontoComboBox);
            }
        });
        extraUnterkontoComboBox.addValueChangeListener(e -> {
            @SuppressWarnings("unchecked")
            List<IdAndName> projekte = new ArrayList<>(((ListDataProvider<IdAndName>) extraProjektComboBox.getDataProvider()).getItems());
            if (projekte.size() == 1 || extraProjektComboBox.isEmpty()) {
                extraProjektComboBox.setValue(projekte.getFirst());
            }
            if (e.isFromClient()) {
                extraUnterkontoComboBox.setOpened(false);
                extraProjektComboBox.focus();
                selectAllText(extraProjektComboBox);
            }
        });
        extraProjektComboBox.addValueChangeListener(e -> {
            if (!e.getHasValue().isEmpty() && e.isFromClient()) {
                extraProjektComboBox.setOpened(false);
                extraBuchungstextField.focus();
            }
        });

        extraBuchungstextField.addClassName("buchungstext_field");
        extraBuchungstextField.setPattern(BUCHUNGSTEXT_PATTERN);
        extraBuchungstextField.setI18n(new TextField.TextFieldI18n()
            .setPatternErrorMessage("Text zu lang"));
        extraBuchungstextField.setValueChangeMode(ValueChangeMode.EAGER);

        Shortcuts.addShortcutListener(extraBetragField, () -> {
                extraHauptkontoComboBox.focus();
                selectAllText(extraHauptkontoComboBox);
            }, Key.ENTER)
            .listenOn(extraBetragField);

        extraHauptkontoComboBox.setAllowCustomValue(true);
        extraHauptkontoComboBox.addCustomValueSetListener(e -> {
            applyFilter(e.getDetail(), extraHauptkontoComboBox);
            extraUnterkontoComboBox.focus();
            selectAllText(extraUnterkontoComboBox);
        });

        Shortcuts.addShortcutListener(extraHauptkontoComboBox, () -> {
                extraHauptkontoComboBox.blur();
                extraUnterkontoComboBox.focus();
                selectAllText(extraUnterkontoComboBox);
            }, Key.ENTER)
            .listenOn(extraHauptkontoComboBox);

        extraUnterkontoComboBox.setAllowCustomValue(true);
        extraUnterkontoComboBox.addCustomValueSetListener(e ->
            applyFilter(e.getDetail(), extraUnterkontoComboBox));
        Shortcuts.addShortcutListener(extraUnterkontoComboBox, () -> {
                extraUnterkontoComboBox.blur();
                extraProjektComboBox.focus();
                selectAllText(extraProjektComboBox);
            }, Key.ENTER)
            .listenOn(extraUnterkontoComboBox);
        extraProjektComboBox.setAllowCustomValue(true);
        extraProjektComboBox.addCustomValueSetListener(e ->
            applyFilter(e.getDetail(), extraProjektComboBox));
        Shortcuts.addShortcutListener(extraProjektComboBox, () -> {
                extraProjektComboBox.blur();
                extraBuchungstextField.focus();
            }, Key.ENTER)
            .listenOn(extraProjektComboBox);
        Shortcuts.addShortcutListener(extraBuchungstextField, (Command) saveButton::focus, Key.ENTER)
            .listenOn(extraBuchungstextField);
    }

    private static AbstractField<?, ?> lastChildField(Component container) {
        List<? extends AbstractField<?, ?>> components = container.getChildren()
            .filter(c -> c instanceof AbstractField)
            .map(c -> (AbstractField<?, ?>) c)
            .toList();
        return components.isEmpty() ? null : components.getLast();
    }

    @SuppressWarnings("unchecked")
    private static void applyFilter(String filter, ComboBox<IdAndName> current) {
        String currentId = current.getId().orElseThrow();
        String idFilter = filter == null
            ? null
            : EVERYTHING_AFTER_SPACE.matcher(filter).replaceAll("");
        ((ListDataProvider<IdAndName>) current.getDataProvider()).getItems().stream()
            .filter(e -> String.valueOf(e.getId()).equals(idFilter))
            .findAny()
            .ifPresentOrElse(e -> {
                // filter for ID
                log.debug("{} idFilter={} => element={}", currentId, idFilter, e);
                current.setValue(e);
            }, () -> {
                // filter for entry name
                String filterLowercase = filter == null ? null : filter.toLowerCase();
                List<IdAndName> filtered = ((ListDataProvider<IdAndName>) current.getDataProvider()).getItems().stream()
                    .filter(e -> filterLowercase != null && e.getName().toLowerCase().contains(filterLowercase))
                    .toList();
                if (filtered.size() == 1) {
                    IdAndName found = filtered.getFirst();
                    log.debug("{} filter={} => element={}", currentId, filter, found);
                    current.setValue(found);
                } else {
                    log.debug("{} filter={} idFilter={} => no element found", currentId, filter, idFilter);
                }
            });
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
        if (rr.getResult().size() <= index || rr.getResult().get(index).getHauptkonto() < 0) {
            return null;
        }
        String name = getKontoName(rr.getResult().get(index).getHauptkonto(), 0);
        return new IdAndName(rr.getResult().get(index).getHauptkonto(), name);
    }

    private static void setHauptkonto(RuleResult rr, int index, IdAndName hk) {
        while (rr.getResult().size() <= index) {
            rr.getResult().add(new Buchung(hk.getId(), null, null, null));
        }
        rr.getResult().get(index).setHauptkonto(hk == null ? 0 : hk.getId());
    }

    private IdAndName getUnterkonto(RuleResult rr, int index) {
        if (rr.getResult().size() <= index || rr.getResult().get(index).getUnterkonto() < 0) {
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
        if (rr.getResult().size() <= index || rr.getResult().get(index).getProjekt() < 0) {
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
        rr.getResult().get(index).setBuchungstext(buchungstext == null ? "" : buchungstext);
    }

    private IdAndName getDonationUnterkontoByName(String... nameParts) {
        if (tableOptigemAccounts == null) {
            return null;
        }
        return tableOptigemAccounts.getRows().stream()
            .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnHk).equals(java.lang.String.valueOf(8010))
                && r.get(accountsColumnBez) != null && containsAllCaseInsensitive(r.get(accountsColumnBez), nameParts))
            .map(r -> r.get(accountsColumnUk))
            .findFirst()
            .map(uk -> new IdAndName(Integer.parseInt(uk), getKontoName(8010, Integer.parseInt(uk))))
            .orElse(new IdAndName(0, getKontoName(8010, 0)));
    }

    private static boolean containsAllCaseInsensitive(String toCheck, String... parts) {
        return Stream.of(parts).allMatch(part -> part != null && toCheck.toLowerCase().contains(part.toLowerCase()));
    }

    private String getKontoName(int hk, int uk) {
        if (tableOptigemAccounts == null) {
            return null;
        }
        return tableOptigemAccounts.getRows().stream()
            .filter(r -> r.get(accountsColumnHk) != null && r.get(accountsColumnHk).equals(java.lang.String.valueOf(hk))
                && r.get(accountsColumnUk) != null && r.get(accountsColumnUk).equals(java.lang.String.valueOf(uk)))
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
