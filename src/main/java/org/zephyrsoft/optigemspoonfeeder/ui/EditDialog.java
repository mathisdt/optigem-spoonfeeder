package org.zephyrsoft.optigemspoonfeeder.ui;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
    private final Table tableOptigemProjects;
    private final Holder<Boolean> automaticFocusChangeAllowed = new Holder<>(true);
    private final Holder<Boolean> openHauptkontoComboBox = new Holder<>(true);
    private Runnable hauptkontoValueChangeTask;

    @SuppressWarnings("unchecked")
    public EditDialog(Table tableOptigemAccounts, Table tableOptigemProjects, RuleResult rr, Runnable updateTableRow) {
        this.tableOptigemAccounts = tableOptigemAccounts;
        this.tableOptigemProjects = tableOptigemProjects;

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

        binder.readBean(rr);

        initializing.setValue(false);

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
