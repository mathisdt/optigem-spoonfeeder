package org.zephyrsoft.optigemspoonfeeder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.service.ExportService;
import org.zephyrsoft.optigemspoonfeeder.service.ParseService;
import org.zephyrsoft.optigemspoonfeeder.service.RuleService;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("")
class MainView extends VerticalLayout {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd_hh-mm-ss");

	private final ParseService parseService;
	private final RuleService ruleService;
	private ExportService exportService;

	private String timestamp;
	private String originalFilename;
	private List<RuleResult> result;

	private MultiFileMemoryBuffer buffer;

	private TextArea log;
	private Grid<RuleResult> grid;

	private HorizontalLayout bottom;

	private HeaderRow headerRow;

	MainView(ParseService parseService, RuleService ruleService, ExportService exportService) {
		this.parseService = parseService;
		this.ruleService = ruleService;
		this.exportService = exportService;

		setSizeFull();

		Button reapplyRules = new Button("Regeln erneut anwenden");
		reapplyRules.addClickListener(e -> applyRulesToUploadedFile());
		reapplyRules.setEnabled(false);

		buffer = new MultiFileMemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setMaxFiles(1);
		upload.setWidthFull();
		upload.addSucceededListener(event -> {
			originalFilename = event.getFileName();
			timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
			applyRulesToUploadedFile();
			reapplyRules.setEnabled(true);
		});

		log = new TextArea();
		log.setLabel("Log");
		log.setValueChangeMode(ValueChangeMode.EAGER);
		log.setWidthFull();

		HorizontalLayout top = new HorizontalLayout(upload, reapplyRules, log);
		top.setWidthFull();
		add(top);

		grid = new Grid<>(RuleResult.class, false);
		grid.setSizeFull();
		grid.setSelectionMode(SelectionMode.NONE);
		grid.setPartNameGenerator(rr -> rr.hasBuchung() ? null : "yellow");
		add(grid);

		bottom = new HorizontalLayout();
		bottom.setWidthFull();
		add(bottom);
	}

	private void applyRulesToUploadedFile() {
		InputStream inputStream = buffer.getInputStream(originalFilename);
		convertUploadedFile(inputStream);

		bottom.removeAll();

		StreamResource streamBuchungen = new StreamResource(
				originalFilename.replaceFirst("\\.[^\\.]+$", "") + "_" + timestamp + "_buchungen.xlsx",
				() -> exportService.createBuchungenExport(result));
		Anchor downloadBuchungen = new Anchor(streamBuchungen, "");
		downloadBuchungen.getElement().setAttribute("download", true);
		// hack to make it look like a button:
		downloadBuchungen.removeAll();
		downloadBuchungen.add(new Button("Buchungen", new Icon(VaadinIcon.DOWNLOAD)));

		StreamResource streamRestMt940 = new StreamResource(
				originalFilename.replaceFirst("\\.[^\\.]+$", "") + "_" + timestamp + "_rest.sta",
				() -> exportService.createMt940Export(result));
		Anchor downloadRestMt940 = new Anchor(streamRestMt940, "");
		downloadRestMt940.getElement().setAttribute("download", true);
		// hack to make it look like a button:
		downloadRestMt940.removeAll();
		downloadRestMt940.add(new Button("Rest (MT940)", new Icon(VaadinIcon.DOWNLOAD)));

		bottom.add(downloadBuchungen, downloadRestMt940);
	}

	private void convertUploadedFile(InputStream inputStream) {
		try {
			result = ruleService.apply(parseService.parse(inputStream));
			log.setValue("Ergebnis: " + result.size() + " Buchungen, davon "
					+ result.stream().filter(RuleResult::hasBuchung).count() + " zugeordnet");

			grid.removeAllColumns();
			grid.setItems(new ListDataProvider<>(result));
			configureColumns();
		} catch (IOException e) {
			log.setValue("Error: " + e.getMessage());
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

		Column<RuleResult> resultClearButton = grid
				.addComponentColumn(rr -> {
					Button button = new Button(new Icon(VaadinIcon.CLOSE));
					button.addClickListener(e -> {
						rr.clearBuchung();
						grid.getDataProvider().refreshItem(rr);
					});
					button.setEnabled(rr.hasBuchung());
					return button;
				})
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Löschen");
		Column<RuleResult> resultHauptkonto = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getHauptkonto())
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Hauptkonto");
		Column<RuleResult> resultUnterkonto = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getUnterkonto())
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Unterkonto");
		Column<RuleResult> resultProjekt = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getProjekt())
				.setFlexGrow(1)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Projekt");
		Column<RuleResult> resultBuchungstext = grid
				.addColumn(rr -> rr.getResult() == null ? null : rr.getResult().getBuchungstext())
				.setTooltipGenerator(rr -> rr.getResult() == null ? null : rr.getResult().getBuchungstext())
				.setFlexGrow(2)
				.setAutoWidth(true)
				.setResizable(true)
				.setHeader("Buchungstext");

		if (headerRow == null) {
			headerRow = grid.prependHeaderRow();
			headerRow.join(sourceDate, sourceAccount, sourceName, sourceText, sourceAmount, sourceBuchungstext)
					.setText("Kontoumsatz");
			headerRow.join(resultClearButton, resultHauptkonto, resultUnterkonto, resultProjekt, resultBuchungstext)
					.setText("Optigem-Buchung");
		}

	}

	private Renderer<RuleResult> date(ValueProvider<RuleResult, LocalDate> valueProvider) {
		return new LocalDateRenderer<>(valueProvider, "dd.MM.");
	}

	private Renderer<RuleResult> nr(ValueProvider<RuleResult, Number> valueProvider) {
		return new NumberRenderer<>(valueProvider, "%,.2f €", Locale.GERMAN);
	}
}