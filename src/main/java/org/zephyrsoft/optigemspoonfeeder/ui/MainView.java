package org.zephyrsoft.optigemspoonfeeder.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.service.ParseService;
import org.zephyrsoft.optigemspoonfeeder.service.RuleService;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

@Route("")
class MainView extends VerticalLayout {

	private final ParseService parseService;
	private final RuleService ruleService;

	private TextArea log;
	private Grid<RuleResult> grid;

	MainView(ParseService parseService, RuleService ruleService) {
		this.parseService = parseService;
		this.ruleService = ruleService;

		log = new TextArea();
		log.setLabel("Log");
		log.setValueChangeMode(ValueChangeMode.EAGER);
		add(log);

		MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setMaxFiles(1);

		upload.addSucceededListener(event -> {
			String fileName = event.getFileName();
			InputStream inputStream = buffer.getInputStream(fileName);
			convertUploadedFile(inputStream);
		});
		add(upload);

		grid = new Grid<>(RuleResult.class, false);
		grid.setWidthFull();
		grid.setSelectionMode(SelectionMode.MULTI);
		add(grid);
	}

	private void convertUploadedFile(InputStream inputStream) {
		try {
			RulesResult result = ruleService.apply(parseService.parse(inputStream));
			log("Analyzed input file: " + result.getConverted().size() + " entries successfully converted and "
					+ result.getRejected().size() + " rejected");

			grid.removeAllColumns();
			grid.setItems(new ListDataProvider<>(result.getConverted()));
			grid.addItemClickListener(e -> {
				if (grid.getSelectionModel().isSelected(e.getItem())) {
					grid.deselect(e.getItem());
				} else {
					grid.select(e.getItem());
				}
			});
			configureColumns();
		} catch (IOException e) {
			log("Error: " + e.getMessage());
		}
	}

	private void configureColumns() {
		Column<RuleResult> sourceDate = grid.addColumn(rr -> rr.getInput().getValutaDatum())
				.setFlexGrow(1)
				.setHeader("Datum");
		Column<RuleResult> sourceAccount = grid.addColumn(rr -> rr.getInput().getKontoNummer())
				.setTooltipGenerator(rr -> rr.getInput().getKontoNummer())
				.setFlexGrow(1)
				.setHeader("Kontonummer");
		Column<RuleResult> sourceName = grid.addColumn(rr -> rr.getInput().getName())
				.setTooltipGenerator(rr -> rr.getInput().getName())
				.setFlexGrow(2)
				.setHeader("Name");
		Column<RuleResult> sourceText = grid.addColumn(rr -> rr.getInput().getVerwendungszweck())
				.setTooltipGenerator(rr -> rr.getInput().getVerwendungszweck())
				.setFlexGrow(1)
				.setHeader("Verwendungszweck");
		Column<RuleResult> sourceAmount = grid.addColumn(nr(rr -> rr.getInput().getBetragMitVorzeichen()))
				.setFlexGrow(3)
				.setPartNameGenerator(rr -> rr.getInput().isCredit() ? "green" : "red")
				.setTextAlign(ColumnTextAlign.END)
				.setHeader("Betrag");

		Column<RuleResult> resultHauptkonto = grid.addColumn(rr -> rr.getResult().getHauptkonto())
				.setFlexGrow(1)
				.setHeader("Hauptkonto");
		Column<RuleResult> resultUnterkonto = grid.addColumn(rr -> rr.getResult().getUnterkonto())
				.setFlexGrow(1)
				.setHeader("Unterkonto");
		Column<RuleResult> resultProjekt = grid.addColumn(rr -> rr.getResult().getProjekt())
				.setFlexGrow(1)
				.setHeader("Projekt");
		Column<RuleResult> resultBuchungstext = grid.addColumn(rr -> rr.getResult().getBuchungstext())
				.setTooltipGenerator(rr -> rr.getResult().getBuchungstext())
				.setFlexGrow(2)
				.setHeader("Buchungstext");

		HeaderRow headerRow = grid.prependHeaderRow();
		headerRow.join(sourceDate, sourceAccount, sourceName, sourceText, sourceAmount)
				.setText("Kontoumsatz");
		headerRow.join(resultHauptkonto, resultUnterkonto, resultProjekt, resultBuchungstext)
				.setText("Optigem-Buchung");

	}

	private Renderer<RuleResult> nr(ValueProvider<RuleResult, Number> valueProvider) {
		return new NumberRenderer<>(valueProvider, "%,.2f €", Locale.GERMAN);
	}

	private void log(String line) {
		log.setValue((StringUtils.isBlank(log.getValue()) ? "" : log.getValue() + "\n") + line);
	}
}