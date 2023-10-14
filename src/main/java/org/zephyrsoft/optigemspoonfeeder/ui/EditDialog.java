package org.zephyrsoft.optigemspoonfeeder.ui;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.Holder;
import org.zephyrsoft.optigemspoonfeeder.model.IdAndName;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("NonSerializableFieldInSerializableClass")
final class EditDialog extends Dialog {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(Locale.GERMAN);
    static {
        CURRENCY_FORMAT.setMinimumIntegerDigits(1);
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setGroupingUsed(true);
    }

    private final Table tableOptigemAccounts;
    private final Table tableOptigemProjects;

    public EditDialog(Table tableOptigemAccounts, Table tableOptigemProjects, RuleResult rr, Runnable updateTableRow) {
        this.tableOptigemAccounts = tableOptigemAccounts;
        this.tableOptigemProjects = tableOptigemProjects;

        setWidth("65%");
        setResizable(true);
        setDraggable(true);
        setCloseOnEsc(true);

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

        ComboBox<IdAndName> hauptkontoComboBox = new ComboBox<>();
        hauptkontoComboBox.setId("hauptkontoComboBox");
        hauptkontoComboBox.setAutoOpen(true);
        hauptkontoComboBox.setWidthFull();
        hauptkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        hauptkontoComboBox.addFocusListener(e -> {
            hauptkontoComboBox.setOpened(true);
        });

        ComboBox<IdAndName> unterkontoComboBox = new ComboBox<>();
        unterkontoComboBox.setId("unterkontoComboBox");
        unterkontoComboBox.setAutoOpen(true);
        unterkontoComboBox.setWidthFull();
        unterkontoComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        unterkontoComboBox.addFocusListener(e -> {
            unterkontoComboBox.setOpened(true);
        });

        ComboBox<IdAndName> projektComboBox = new ComboBox<>();
        projektComboBox.setId("projektComboBox");
        projektComboBox.setAutoOpen(true);
        projektComboBox.setWidthFull();
        projektComboBox.setItemLabelGenerator(e -> e.getId() + " " + e.getName());
        projektComboBox.addFocusListener(e -> {
            projektComboBox.setOpened(true);
        });

        TextField buchungstextField = new TextField();
        buchungstextField.setId("buchungstextField");
        buchungstextField.setWidthFull();

        if (tableOptigemAccounts != null) {
            hauptkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                .filter(r -> r.get("Hauptkonto") != null && r.get("Unterkonto") != null
                    && r.get("Unterkonto").equals("0"))
                .map(r -> new IdAndName(Integer.parseInt(r.get("Hauptkonto")), r.get("Kontobezeichnung")))
                .toList()));
            setCalculatedComboboxDropdownWidth(hauptkontoComboBox);
        }

        if (tableOptigemProjects != null) {
            projektComboBox.setItems(new ListDataProvider<>(tableOptigemProjects.getRows().stream()
                .filter(r -> r.get("Nr") != null && r.get("Name") != null)
                .map(r -> new IdAndName(Integer.parseInt(r.get("Nr")), r.get("Name")))
                .toList()));
            setCalculatedComboboxDropdownWidth(projektComboBox);
        }

        Binder<RuleResult> binder = new Binder<>(RuleResult.class);

        binder.forField(hauptkontoComboBox).bind(this::getHauptkonto, EditDialog::setHauptkonto);
        binder.forField(unterkontoComboBox).bind(this::getUnterkonto, EditDialog::setUnterkonto);
        binder.forField(projektComboBox).bind(this::getProjekt, EditDialog::setProjekt);
        binder.forField(buchungstextField).bind(EditDialog::getBuchungstext, EditDialog::setBuchungstext);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.addClassName("spaced-form");
        add(formLayout);

        formLayout.addFormItem(datum, "Datum");
        formLayout.addFormItem(kontonummer, "Kontonummer");
        formLayout.addFormItem(name, "Name");
        formLayout.addFormItem(verwendungszweck, "Verwendungszweck");
        formLayout.addFormItem(betrag, "Betrag");
        formLayout.addFormItem(buchungstext, "Buchungstext");

        formLayout.addFormItem(hauptkontoComboBox, "Hauptkonto");
        formLayout.addFormItem(unterkontoComboBox, "Unterkonto");
        formLayout.addFormItem(projektComboBox, "Projekt");
        formLayout.addFormItem(buchungstextField, "Buchungstext");

        Button saveButton = new Button("Speichern & Schließen", e -> {
            try {
                binder.writeBean(rr);
                updateTableRow.run();
                close();
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button deleteButton = new Button("Löschen & Schließen", e -> {
            rr.setResult(null);
            updateTableRow.run();
            close();
        });
        deleteButton.setTabIndex(-1);
        HorizontalLayout buttons = new HorizontalLayout(FlexComponent.JustifyContentMode.BETWEEN, saveButton, deleteButton);
        add(buttons);

        final Holder<Boolean> initializing = new Holder<>(true);

        hauptkontoComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && tableOptigemAccounts != null) {
                unterkontoComboBox.setItems(new ListDataProvider<>(tableOptigemAccounts.getRows().stream()
                    .filter(r -> r.get("Hauptkonto") != null && r.get("Unterkonto") != null
                        && r.get("Hauptkonto").equals(String.valueOf(e.getValue().getId())))
                    .map(r -> new IdAndName(Integer.parseInt(r.get("Unterkonto")), r.get("Kontobezeichnung")))
                    .toList()));
                setCalculatedComboboxDropdownWidth(unterkontoComboBox);
            } else {
                unterkontoComboBox.setItems(Collections.emptyList());
            }

            int unterkontoOptionCount = ((ListDataProvider<?>) unterkontoComboBox.getDataProvider()).getItems().size();
            if (unterkontoOptionCount == 1) {
                unterkontoComboBox.setValue(
                    (IdAndName) ((ListDataProvider<?>) unterkontoComboBox.getDataProvider()).getItems().stream().toList().get(0));
            } else if (!e.getHasValue().isEmpty() && !initializing.getValue()) {
                hauptkontoComboBox.setOpened(false);
                unterkontoComboBox.focus();
            }
        });
        unterkontoComboBox.addValueChangeListener(e -> {
            int projektOptionCount = ((ListDataProvider<?>) projektComboBox.getDataProvider()).getItems().size();
            if (projektOptionCount == 1) {
                projektComboBox.setValue(
                    (IdAndName) ((ListDataProvider<?>) projektComboBox.getDataProvider()).getItems().stream().toList().get(0));
            } else if (!e.getHasValue().isEmpty() && !initializing.getValue()) {
                unterkontoComboBox.setOpened(false);
                projektComboBox.focus();
            }
        });
        projektComboBox.addValueChangeListener(e -> {
            if (!e.getHasValue().isEmpty() && !initializing.getValue()) {
                projektComboBox.setOpened(false);
                buchungstextField.focus();
            }
        });

        binder.readBean(rr);

        initializing.setValue(false);

        Shortcuts.addShortcutListener(hauptkontoComboBox, () -> applyFilterToId(hauptkontoComboBox), Key.ENTER)
            .listenOn(hauptkontoComboBox);
        Shortcuts.addShortcutListener(unterkontoComboBox, () -> applyFilterToId(unterkontoComboBox), Key.ENTER)
            .listenOn(unterkontoComboBox);
        Shortcuts.addShortcutListener(projektComboBox, () -> applyFilterToId(projektComboBox), Key.ENTER)
            .listenOn(projektComboBox);
        Shortcuts.addShortcutListener(buchungstextField, saveButton::focus, Key.ENTER)
            .listenOn(buchungstextField);
    }

    @SuppressWarnings("unchecked")
    private static void applyFilterToId(final ComboBox<IdAndName> current) {
        String currentId = current.getId().orElseThrow();
        log.debug("{} filtering", currentId);
        UI.getCurrent().getPage().executeJs("return document.getElementById('" + currentId + "').getElementsByTagName('input')[0].value;")
            .then(String.class, str -> {
                log.debug("{} filter={}", currentId, str);
                ((ListDataProvider<IdAndName>) current.getDataProvider()).getItems().stream()
                    .filter(e -> String.valueOf(e.getId()).equals(str))
                    .findAny()
                    .ifPresentOrElse(e -> {
                        log.debug("{} filter={} => element={}", currentId, str, e);
                        current.setValue(e);
                    }, () -> {
                        log.debug("{} filter={} => no element found", currentId, str);
                    });
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

    private IdAndName getHauptkonto(RuleResult rr) {
        if (rr.getResult() == null) {
            return null;
        }
        String name = getKontoName(rr.getResult().getHauptkonto(), 0);
        return new IdAndName(rr.getResult().getHauptkonto(), name);
    }

    private static void setHauptkonto(RuleResult rr, IdAndName hk) {
        if (rr.getResult() == null) {
            rr.setResult(new Buchung(hk.getId(), null, null, null));
        }
        rr.getResult().setHauptkonto(hk.getId());
    }

    private IdAndName getUnterkonto(RuleResult rr) {
        if (rr.getResult() == null) {
            return null;
        }
        String name = getKontoName(rr.getResult().getHauptkonto(), rr.getResult().getUnterkonto());
        return new IdAndName(rr.getResult().getUnterkonto(), name);
    }

    private static void setUnterkonto(RuleResult rr, IdAndName uk) {
        if (rr.getResult() == null) {
            rr.setResult(new Buchung(null, uk.getId(), null, null));
        }
        rr.getResult().setUnterkonto(uk == null ? 0 : uk.getId());
    }

    private IdAndName getProjekt(RuleResult rr) {
        if (rr.getResult() == null) {
            return null;
        }
        String name = getProjektName(rr.getResult().getProjekt());
        return new IdAndName(rr.getResult().getProjekt(), name);
    }

    private static void setProjekt(RuleResult rr, IdAndName proj) {
        if (rr.getResult() == null) {
            rr.setResult(new Buchung(null, null, proj.getId(), null));
        }
        rr.getResult().setProjekt(proj == null ? 0 : proj.getId());
    }

    private static String getBuchungstext(RuleResult rr) {
        if (rr.getResult() == null) {
            return null;
        }
        return rr.getResult().getBuchungstext();
    }

    private static void setBuchungstext(RuleResult rr, String buchungstext) {
        if (rr.getResult() == null) {
            rr.setResult(new Buchung(null, null, null, buchungstext));
        }
        rr.getResult().setBuchungstext(buchungstext);
    }

    private String getKontoName(int hk, int uk) {
        if (tableOptigemAccounts == null) {
            return null;
        }
        return tableOptigemAccounts.getRows().stream()
            .filter(r -> r.get("Hauptkonto") != null && r.get("Hauptkonto").equals(String.valueOf(hk))
                && r.get("Unterkonto") != null && r.get("Unterkonto").equals(String.valueOf(uk)))
            .map(r -> r.get("Kontobezeichnung"))
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
