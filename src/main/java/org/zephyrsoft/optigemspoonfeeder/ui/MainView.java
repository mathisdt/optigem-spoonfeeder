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
import org.zephyrsoft.optigemspoonfeeder.model.Konto;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940File;
import org.zephyrsoft.optigemspoonfeeder.service.ExportService;
import org.zephyrsoft.optigemspoonfeeder.service.HibiscusImportService;
import org.zephyrsoft.optigemspoonfeeder.service.ParseService;
import org.zephyrsoft.optigemspoonfeeder.service.PersistenceService;
import org.zephyrsoft.optigemspoonfeeder.service.RuleService;

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
final class MainView extends VerticalLayout {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final Pattern PATTERN = Pattern.compile("\\.[^\\.]+$");

	private final ParseService parseService;
	private final RuleService ruleService;
	private final ExportService exportService;
	private final PersistenceService persistenceService;

	private String timestamp;
	private Mt940File parsed;
	private Table tableOptigemAccounts;
	private Table tableOptigemProjects;
	private String originalFilename;
	private RulesResult result;

	private Div logArea;
	private Grid<RuleResult> grid;
	private Span logText;

	private HorizontalLayout buttons;

	private HeaderRow headerRow;

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
					loadTables(account);
					applyRulesToParsedData();
					reapplyRules.setEnabled(true);
				});
		loadFromHibiscusServerButton.setEnabled(hibiscusConfiguredAndReachable);

		VerticalLayout loadFromHibiscusServer = new VerticalLayout(month, account, loadFromHibiscusServerButton);
		loadFromHibiscusServer.setPadding(false);

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
			reapplyRules.setEnabled(true);
		});

		logArea = new Div();
		logArea.setWidthFull();
		Scroller scroller = new Scroller(logArea);
		scroller.setSizeFull();

		HorizontalLayout topLeft = new HorizontalLayout(upload, loadFromHibiscusServer, reapplyRules);
		topLeft.setPadding(false);
		HorizontalLayout top = new HorizontalLayout(topLeft, scroller);
		top.setWidthFull();
		add(top);

		grid = new Grid<>(RuleResult.class, false);
		grid.setSizeFull();
		grid.setSelectionMode(SelectionMode.NONE);
		grid.setPartNameGenerator(rr -> rr.hasBuchung() ? null : "yellow");
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

	private void loadTables(ComboBox<Konto> account) {
		List<Table> tables = persistenceService.getTables();
		if (StringUtils.isNotBlank(account.getValue().getTableAccounts())) {
			tableOptigemAccounts = tables.stream()
				.filter(t -> t.getName().equalsIgnoreCase(account.getValue().getTableAccounts()))
				.findAny()
				.orElseGet(()-> {
					log.warn("konfigurierte Konten-Tabelle {} nicht gefunden", account.getValue().getTableAccounts());
					return null;
				});
		}
		if (StringUtils.isNotBlank(account.getValue().getTableProjects())) {
			tableOptigemProjects = tables.stream()
				.filter(t -> t.getName().equalsIgnoreCase(account.getValue().getTableProjects()))
				.findAny()
				.orElseGet(()-> {
					log.warn("konfigurierte Projekte-Tabelle {} nicht gefunden", account.getValue().getTableProjects());
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
		Anchor downloadBuchungen = new Anchor(streamBuchungen, "");
		downloadBuchungen.getElement().setAttribute("download", true);
		// hack to make it look like a button:
		downloadBuchungen.removeAll();
		downloadBuchungen.add(new Button("Buchungen", new Icon(VaadinIcon.DOWNLOAD)));

		StreamResource streamRestMt940 = new StreamResource(
				PATTERN.matcher(originalFilename).replaceFirst("") + "_Stand_" + timestamp + "_rest.sta",
				() -> exportService.createMt940Export(result.getResults()));
		Anchor downloadRestMt940 = new Anchor(streamRestMt940, "");
		downloadRestMt940.getElement().setAttribute("download", true);
		// hack to make it look like a button:
		downloadRestMt940.removeAll();
		downloadRestMt940.add(new Button("Rest (MT940)", new Icon(VaadinIcon.DOWNLOAD)));

		buttons.add(downloadBuchungen, downloadRestMt940);
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
			logText.setText(result.size() + " Buchungen geladen, davon "
					+ result.stream().filter(RuleResult::hasBuchung).count() + " zugeordnet");
			logArea.setText(result.getLogMessages());

			grid.removeAllColumns();
			grid.setItems(new ListDataProvider<>(result.getResults()));
			configureColumns();
		} catch (Exception e) {
			logText.setText("Fehler: " + e.getMessage());
			log.warn("Fehler bei der Releanwendung", e);
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
						EditDialog editDialog = new EditDialog(tableOptigemAccounts, tableOptigemProjects, rr,
							() -> grid.getDataProvider().refreshItem(rr));
						editDialog.open();
					});
					return button;
				})
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Bearbeiten");
		Column<RuleResult> resultHauptkonto = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getHauptkonto())
				.setKey("hauptkonto")
				.setTooltipGenerator(rr -> rr.getResult() == null ? null : getKontoName(rr.getResult().getHauptkonto(), 0))
				.setFlexGrow(1)
				.setWidth("110px")
				.setResizable(true)
				.setHeader("Hauptkonto");
		Column<RuleResult> resultUnterkonto = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getUnterkonto())
				.setKey("unterkonto")
				.setTooltipGenerator(rr -> rr.getResult() == null ? null : getKontoName(rr.getResult().getHauptkonto(), rr.getResult().getUnterkonto()))
				.setFlexGrow(1)
				.setWidth("110px")
				.setResizable(true)
				.setHeader("Unterkonto");
		Column<RuleResult> resultProjekt = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getProjekt())
				.setKey("projekt")
				.setTooltipGenerator(rr -> rr.getResult() == null ? null : getProjektName(rr.getResult().getProjekt()))
				.setFlexGrow(1)
				.setWidth("100px")
				.setResizable(true)
				.setHeader("Projekt");
		Column<RuleResult> resultBuchungstext = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getBuchungstext())
				.setKey("buchungstext")
				.setTooltipGenerator(rr -> rr.getResult() == null ? null : rr.getResult().getBuchungstext())
				.setFlexGrow(2)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Buchungstext");

		if (headerRow == null) {
			headerRow = grid.prependHeaderRow();
			headerRow.join(sourceDate, sourceAccount, sourceName, sourceText, sourceAmount, sourceBuchungstext)
					.setText("Kontoumsatz");
			headerRow.join(resultEditButton, resultHauptkonto, resultUnterkonto, resultProjekt, resultBuchungstext)
					.setText("Optigem-Buchung");
		}
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

	private static Renderer<RuleResult> date(ValueProvider<RuleResult, LocalDate> valueProvider) {
		return new LocalDateRenderer<>(valueProvider, "dd.MM.");
	}

	private static Renderer<RuleResult> nr(ValueProvider<RuleResult, Number> valueProvider) {
		return new NumberRenderer<>(valueProvider, "%,.2f €", Locale.GERMAN);
	}
}
