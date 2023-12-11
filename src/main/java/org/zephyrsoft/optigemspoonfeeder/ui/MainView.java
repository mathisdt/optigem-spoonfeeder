package org.zephyrsoft.optigemspoonfeeder.ui;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.Konto;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.service.ExportService;
import org.zephyrsoft.optigemspoonfeeder.service.HibiscusImportService;
import org.zephyrsoft.optigemspoonfeeder.service.ParseService;
import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;
import org.zephyrsoft.optigemspoonfeeder.service.RuleService;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.UploadI18N.Uploading;
import com.vaadin.flow.component.upload.UploadI18N.Uploading.Status;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import lombok.extern.slf4j.Slf4j;

@Route("")
@PageTitle("Optigem-Spoonfeeder")
@Slf4j
@SuppressWarnings({ "NonSerializableFieldInSerializableClass", "unused" })
final class MainView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern PATTERN = Pattern.compile("\\.[^\\.]+$");

    private final ParseService parseService;
    private final RuleService ruleService;
    private final ExportService exportService;
    private final PersistenceService persistenceService;

    private String timestamp;
    private SourceFile parsed;
    private Table tableOptigemAccounts;
    private String accountsColumnHk;
    private String accountsColumnUk;
    private String accountsColumnBez;
    private Table tableOptigemProjects;
    private String projectsColumnNr;
    private String projectsColumnBez;
    private String originalFilename;
    private RulesResult result;

    private final Div logArea;
    private final Grid<RuleResult> grid;
    private final Span logText;

    private final HorizontalLayout buttons;

    private HeaderRow headerRow;
    private Anchor downloadBuchungen;
    private Anchor downloadRestMt940;

    MainView(ParseService parseService, RuleService ruleService, ExportService exportService,
        HibiscusImportService hibiscusImportService, PersistenceService persistenceService,
        OptigemSpoonfeederProperties properties) {
        this.parseService = parseService;
        this.ruleService = ruleService;
        this.exportService = exportService;
        this.persistenceService = persistenceService;

        setSizeFull();

        Button reapplyRules = new Button("Regeln erneut anwenden");
        reapplyRules.addClickListener(e -> applyRulesToParsedData());
        reapplyRules.setEnabled(false);

        boolean hibiscusConfiguredAndReachable = hibiscusImportService.isConfiguredAndReachable();

        ComboBox<YearMonth> month = new ComboBox<>();
        month.setWidthFull();
        YearMonth currentMonth = YearMonth.now();
        month.setItems(availableMonths());
        final DateTimeFormatter yearMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        month.setItemLabelGenerator(
            ym -> yearMonthFormatter.format(ym) + (ym.equals(currentMonth) ? " (unvollständig)" : ""));
        month.setValue(YearMonth.from(LocalDate.now().minusMonths(1)));

        ComboBox<Konto> account = new ComboBox<>();
        account.setWidthFull();
        if (hibiscusConfiguredAndReachable) {
            List<Konto> konten = hibiscusImportService.getKonten();
            account.setItems(konten);
            if (!konten.isEmpty()) {
                account.setValue(konten.get(0));
            }
        }
        account.setItemLabelGenerator(Konto::getBezeichnung);

        Button loadFromHibiscusServerButton = new Button("von Hibiscus Server laden");
        loadFromHibiscusServerButton
            .addClickListener(e -> {
                loadAndParseFromHibiscus(hibiscusImportService, month, account);
                loadTables(account.getValue().getTableAccounts(), account.getValue().getTableProjects());

                OptigemSpoonfeederProperties.AccountProperties accountProperties = properties.getBankAccount().get(account.getValue().getIban());
                if (accountProperties == null) {
                    accountProperties = properties.getBankAccountByDescription(account.getValue().getBezeichnung());
                }
                if (accountProperties != null) {
                    accountsColumnHk = accountProperties.getAccountsColumnHk();
                    accountsColumnUk = accountProperties.getAccountsColumnUk();
                    accountsColumnBez = accountProperties.getAccountsColumnBez();

                    projectsColumnNr = accountProperties.getProjectsColumnNr();
                    projectsColumnBez = accountProperties.getProjectsColumnBez();
                }

                applyRulesToParsedData();
                reapplyRules.setEnabled(true);
            });
        loadFromHibiscusServerButton.setEnabled(hibiscusConfiguredAndReachable);

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        Button uploadButton = new Button("MT940-Datei einlesen");
        upload.setUploadButton(uploadButton);
        UploadI18N uploadI18N = new UploadI18N();
        Uploading uploading = new Uploading();
        Status status = new Status();
        status.setProcessing("Verarbeite Datei...");
        uploading.setStatus(status);
        uploadI18N.setUploading(uploading);
        upload.setI18n(uploadI18N);
        upload.setMaxFiles(1);
        upload.setDropAllowed(false);
        upload.setWidthFull();
        upload.addSucceededListener(event -> {
            parseUploadedFile(buffer.getInputStream(event.getFileName()), event.getFileName());
            applyRulesToParsedData();
            if (!result.getResults().isEmpty()) {
                String konto = result.getResults().get(0).getInput().getKontobezeichnung();
                OptigemSpoonfeederProperties.AccountProperties accountProperties = properties.getBankAccount().get(konto);
                if (accountProperties == null) {
                    accountProperties = properties.getBankAccountByDescription(konto);
                }
                if (accountProperties != null) {
                    log.debug("load tables for bank account {}", accountProperties);
                    loadTables(accountProperties.getTableAccounts(), accountProperties.getTableProjects());

                    accountsColumnHk = accountProperties.getAccountsColumnHk();
                    accountsColumnUk = accountProperties.getAccountsColumnUk();
                    accountsColumnBez = accountProperties.getAccountsColumnBez();

                    projectsColumnNr = accountProperties.getProjectsColumnNr();
                    projectsColumnBez = accountProperties.getProjectsColumnBez();
                }
            }
            reapplyRules.setEnabled(true);
        });

        logArea = new Div();
        logArea.setWidthFull();
        Scroller scroller = new Scroller(logArea);
        scroller.setSizeFull();

        HorizontalLayout topLeft = new HorizontalLayout(upload, month, account, loadFromHibiscusServerButton, reapplyRules);
        topLeft.setPadding(false);
        HorizontalLayout top = new HorizontalLayout(topLeft, scroller);
        top.setWidthFull();
        add(top);

        grid = new Grid<>(RuleResult.class, false);
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.setPartNameGenerator(rr -> rr.hasBuchungenForWholeSum() ? null : "yellow");
        add(grid);

        buttons = new HorizontalLayout();
        buttons.setWidthFull();
        logText = new Span("noch keine Daten geladen");
        logText.addClassName("right");
        logText.setWidthFull();
        HorizontalLayout bottom = new HorizontalLayout(buttons, logText);
        bottom.setWidthFull();
        add(bottom);

        if (properties.getDir() == null) {
            logArea.setText(
                "Achtung - bitte Konfiguration überprüfen! Das Verzeichnis für Regeln und Tabellen ist nicht konfiguriert.");
        } else if (!Files.exists(properties.getDir())) {
            logArea.setText(
                "Achtung - bitte Konfiguration überprüfen! Das Verzeichnis für Regeln und Tabellen kann nicht geöffnet werden: "
                    + properties.getDir());
        }
    }

    private void loadAndParseFromHibiscus(HibiscusImportService hibiscusImportService, ComboBox<YearMonth> month,
        ComboBox<Konto> account) {
        try {
            originalFilename = account.getValue().getBezeichnungForFilename() + "_" + MONTH_FORMAT.format(month.getValue()) + ".hibiscus";
            timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
            parsed = hibiscusImportService.read(month.getValue(), account.getValue());
        } catch (Exception e) {
            logText.setText("Fehler: " + e.getMessage());
            log.warn("Fehler beim Laden von Hibiscus", e);
        }
    }

    private void loadTables(String accountTableName, String projectsTableName) {
        List<Table> tables = persistenceService.getTables();
        if (StringUtils.isNotBlank(accountTableName)) {
            tableOptigemAccounts = tables.stream()
                .filter(t -> t.getName().equalsIgnoreCase(accountTableName))
                .findAny()
                .orElseGet(() -> {
                    log.warn("konfigurierte Konten-Tabelle {} nicht gefunden", accountTableName);
                    return null;
                });
        }
        if (StringUtils.isNotBlank(projectsTableName)) {
            tableOptigemProjects = tables.stream()
                .filter(t -> t.getName().equalsIgnoreCase(projectsTableName))
                .findAny()
                .orElseGet(() -> {
                    log.warn("konfigurierte Projekte-Tabelle {} nicht gefunden", projectsTableName);
                    return null;
                });
        }
    }

    private static List<YearMonth> availableMonths() {
        List<YearMonth> monthList = new ArrayList<>();
        LocalDate limit = LocalDate.now().minusYears(1).withDayOfYear(1);
        LocalDate current = LocalDate.now();
        while (current.isAfter(limit)) {
            monthList.add(YearMonth.from(current));
            current = current.minusMonths(1);
        }
        return monthList;
    }

    private void applyRulesToParsedData() {
        convertParsedData();

        buttons.removeAll();

        StreamResource streamBuchungen = new StreamResource(
            PATTERN.matcher(originalFilename).replaceFirst("") + "_Stand_" + timestamp + "_buchungen.xlsx",
            () -> exportService.createBuchungenExport(result.getResults()));
        downloadBuchungen = new Anchor(streamBuchungen, "");
        downloadBuchungen.getElement().setAttribute("download", true);
        // hack to make it look like a button:
        downloadBuchungen.removeAll();
        downloadBuchungen.add(new Button("Buchungen", new Icon(VaadinIcon.DOWNLOAD)));

        StreamResource streamRestMt940 = new StreamResource(
            PATTERN.matcher(originalFilename).replaceFirst("") + "_Stand_" + timestamp + "_rest.sta",
            () -> ExportService.createMt940Export(result.getResults()));
        downloadRestMt940 = new Anchor(streamRestMt940, "");
        downloadRestMt940.getElement().setAttribute("download", true);
        // hack to make it look like a button:
        downloadRestMt940.removeAll();
        downloadRestMt940.add(new Button("Rest (MT940)", new Icon(VaadinIcon.DOWNLOAD)));

        buttons.add(downloadBuchungen, downloadRestMt940);

        updateFooter();
    }

    private void parseUploadedFile(InputStream inputStream, String filename) {
        try {
            originalFilename = filename;
            timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
            parsed = parseService.parse(inputStream);
        } catch (Exception e) {
            logText.setText("Fehler: " + e.getMessage());
            log.warn("Fehler beim Parsen", e);
        }
    }

    private void convertParsedData() {
        try {
            result = ruleService.apply(parsed);
            logArea.setText(result.getLogMessages());
            updateFooter();

            grid.removeAllColumns();
            grid.setItems(new ListDataProvider<>(result.getResults()));
            configureColumns();
        } catch (Exception e) {
            logText.setText("Fehler: " + e.getMessage());
            log.warn("Fehler bei der Regelanwendung", e);
        }
    }
    private void updateFooter() {
        int allPostings = result == null ? 0 : result.size();
        long mappedPostings = result == null ? 0 : result.stream().filter(RuleResult::hasBuchung).count();
        logText.setText(allPostings + " Buchungen geladen, davon " + mappedPostings + " zugeordnet");
        if (downloadBuchungen != null) {
            downloadBuchungen.setEnabled(mappedPostings > 0);
        }
        if (downloadRestMt940 != null) {
            downloadRestMt940.setEnabled(mappedPostings < allPostings);
        }
    }

    private void configureColumns() {
        Column<RuleResult> sourceDate = grid
            .addColumn(date(rr -> rr.getInput().getValutaDatum()))
            .setFlexGrow(1)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Datum");
        Column<RuleResult> sourceAccount = grid
            .addColumn(rr -> rr.getInput().getKontoNummer())
            .setTooltipGenerator(rr -> rr.getInput().getKontoNummer())
            .setFlexGrow(1)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Kontonummer");
        Column<RuleResult> sourceName = grid
            .addColumn(rr -> rr.getInput().getName())
            .setTooltipGenerator(rr -> rr.getInput().getName())
            .setFlexGrow(2)
            .setWidth("300px")
            .setResizable(true)
            .setHeader("Name");
        Column<RuleResult> sourceText = grid
            .addColumn(rr -> rr.getInput().getVerwendungszweckClean())
            .setTooltipGenerator(rr -> rr.getInput().getVerwendungszweckClean())
            .setFlexGrow(1)
            .setResizable(true)
            .setWidth("300px")
            .setHeader("Verwendungszweck");
        Column<RuleResult> sourceAmount = grid
            .addColumn(nr(rr -> rr.getInput().getBetragMitVorzeichen()))
            .setFlexGrow(3)
            .setAutoWidth(true)
            .setResizable(true)
            .setPartNameGenerator(rr -> rr.getInput().isCredit() ? "green" : "red")
            .setTextAlign(ColumnTextAlign.END)
            .setHeader("Betrag");
        Column<RuleResult> sourceBuchungstext = grid
            .addColumn(rr -> rr.getInput().getBuchungstextClean())
            .setTooltipGenerator(rr -> rr.getInput().getBuchungstextClean())
            .setFlexGrow(1)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Buchungstext");

        Column<RuleResult> resultEditButton = grid
            .addComponentColumn(rr -> {
                Button button = new Button(new Icon(VaadinIcon.EDIT));
                button.addClickListener(e -> {
                    EditDialog editDialog = new EditDialog(tableOptigemAccounts, accountsColumnHk, accountsColumnUk, accountsColumnBez,
                        tableOptigemProjects, projectsColumnNr, projectsColumnBez, rr,
                        () -> {
                            grid.getDataProvider().refreshItem(rr);
                            updateFooter();
                        });
                    editDialog.open();
                });
                return button;
            })
            .setFlexGrow(1)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Bearbeiten");
        Column<RuleResult> resultBetrag = grid
            .addComponentColumn(rr -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setSizeFull();
                for (Buchung b : rr.getResult()) {
                    String buchungsBetrag = String.format(Locale.GERMAN, "%,.2f €", b.getBetrag());
                    Span span = new Span();
                    span.setText(buchungsBetrag);
                    span.setTitle(buchungsBetrag);
                    span.addClassName("absolute-height");
                    layout.add(span);
                }
                return layout;
            })
            .setKey("betrag")
            .setFlexGrow(2)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Betrag");
        Column<RuleResult> resultHauptkonto = grid
            .addComponentColumn(rr -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setSizeFull();
                for (Buchung b : rr.getResult()) {
                    Span main = new Span(rr.getResult() == null ? "" : String.valueOf(b.getHauptkonto()));
                    String kontoName = getKontoName(b.getHauptkonto(), 0);
                    Span sub = new Span(rr.getResult() == null ? "" : kontoName);
                    main.setTitle(kontoName);
                    sub.setTitle(kontoName);
                    sub.setWidthFull();
                    sub.addClassName("small-text-with-ellipsis");
                    VerticalLayout item = new VerticalLayout(main, sub);
                    item.setPadding(false);
                    item.setSpacing(false);
                    item.addClassName("absolute-height");
                    layout.add(item);
                }
                return layout;
            })
            .setKey("hauptkonto")
            .setFlexGrow(1)
            .setWidth("110px")
            .setResizable(true)
            .setHeader("Hauptkonto");
        Column<RuleResult> resultUnterkonto = grid
            .addComponentColumn(rr -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setSizeFull();
                for (Buchung b : rr.getResult()) {
                    Span main = new Span(rr.getResult() == null ? "" : String.valueOf(b.getUnterkonto()));
                    String kontoName = getKontoName(b.getHauptkonto(), b.getUnterkonto());
                    Span sub = new Span(rr.getResult() == null ? "" : kontoName);
                    main.setTitle(kontoName);
                    sub.setTitle(kontoName);
                    sub.setWidthFull();
                    sub.addClassName("small-text-with-ellipsis");
                    VerticalLayout item = new VerticalLayout(main, sub);
                    item.setPadding(false);
                    item.setSpacing(false);
                    item.addClassName("absolute-height");
                    layout.add(item);
                }
                return layout;
            })
            .setKey("unterkonto")
            .setFlexGrow(1)
            .setWidth("110px")
            .setResizable(true)
            .setHeader("Unterkonto");
        Column<RuleResult> resultProjekt = grid
            .addComponentColumn(rr -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setSizeFull();
                for (Buchung b : rr.getResult()) {
                    Span main = new Span(rr.getResult() == null ? "" : String.valueOf(b.getProjekt()));
                    String projektName = getProjektName(b.getProjekt());
                    Span sub = new Span(rr.getResult() == null ? "" : projektName);
                    main.setTitle(projektName);
                    sub.setTitle(projektName);
                    sub.setWidthFull();
                    sub.addClassName("small-text-with-ellipsis");
                    VerticalLayout item = new VerticalLayout(main, sub);
                    item.setPadding(false);
                    item.setSpacing(false);
                    item.addClassName("absolute-height");
                    layout.add(item);
                }
                return layout;
            })
            .setKey("projekt")
            .setFlexGrow(1)
            .setWidth("100px")
            .setResizable(true)
            .setHeader("Projekt");
        Column<RuleResult> resultBuchungstext = grid
            .addComponentColumn(rr -> {
                VerticalLayout layout = new VerticalLayout();
                layout.setSpacing(false);
                layout.setPadding(false);
                layout.setSizeFull();
                for (Buchung b : rr.getResult()) {
                    String buchungstext = b.getBuchungstext();
                    Span span = new Span(buchungstext);
                    span.setTitle(buchungstext);
                    span.addClassName("absolute-height");
                    layout.add(span);
                }
                return layout;
            })
            .setKey("buchungstext")
            .setFlexGrow(2)
            .setAutoWidth(true)
            .setResizable(true)
            .setHeader("Buchungstext");

        if (headerRow == null) {
            headerRow = grid.prependHeaderRow();
            headerRow.join(sourceDate, sourceAccount, sourceName, sourceText, sourceAmount, sourceBuchungstext)
                .setText("Kontoumsatz");
            headerRow.join(resultEditButton, resultBetrag, resultHauptkonto, resultUnterkonto, resultProjekt, resultBuchungstext)
                .setText("Optigem-Buchung");
        }
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
            .filter(r -> r.get(projectsColumnNr) != null && r.get(projectsColumnNr).equals(String.valueOf(nr)))
            .map(r -> r.get(projectsColumnBez))
            .findFirst()
            .orElse(null);
    }

    private static Renderer<RuleResult> date(ValueProvider<RuleResult, LocalDate> valueProvider) {
        return new LocalDateRenderer<>(valueProvider, "dd.MM.");
    }

    private static Renderer<RuleResult> nr(ValueProvider<RuleResult, Number> valueProvider) {
        return new NumberRenderer<>(valueProvider, "%,.2f €", Locale.GERMAN);
    }
}
