package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;

import com.coreoz.windmill.Windmill;
import com.coreoz.windmill.exports.config.ExportHeaderMapping;
import com.coreoz.windmill.exports.config.ExportRowsConfig;
import com.coreoz.windmill.exports.exporters.csv.ExportCsvConfig;
import com.coreoz.windmill.exports.exporters.excel.ExportExcelConfig;
import com.coreoz.windmill.files.FileSource;
import com.coreoz.windmill.imports.Row;
import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

	private static class GsonExclusionStrategy implements ExclusionStrategy {

		@Override
		public boolean shouldSkipField(final FieldAttributes f) {
			// "waehrung" is filled in the constructor (which we use by configuring GSON with "disableJdkUnsafe"), so we don't need it here
			return f.getDeclaringClass().equals(SourceEntry.class)
				&& f.getDeclaredClass().equals(NumberFormat.class)
				&& f.getName().equals(SourceEntry.Fields.waehrung);
		}
		@Override
		public boolean shouldSkipClass(final Class<?> clazz) {
			return false;
		}
	}

	private static final String RULES_FILENAME = "rules.groovy";
	private static final String DATA_SUBDIR = "saved-months";
	private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final Pattern JSON_FILE_NAME = Pattern.compile("^\\d{4}-\\d{2}.json$");
	private static final Pattern JSON_FILE_EXTENSION = Pattern.compile(".json$");

	private final OptigemSpoonfeederProperties properties;

	private Gson gson;

	@PostConstruct
	public void initStorage() {
		GsonExclusionStrategy exclusionStrategy = new GsonExclusionStrategy();
		gson = Converters.registerAll(new GsonBuilder())
			.disableJdkUnsafe()
			.addSerializationExclusionStrategy(exclusionStrategy)
			.addDeserializationExclusionStrategy(exclusionStrategy)
			.create();
	}

	public String getRules() {
		try {
			return Files.readString(properties.getDir().resolve(RULES_FILENAME));
		} catch (IOException e) {
			throw new IllegalStateException("could not read rules from " + properties.getDir() + "/" + RULES_FILENAME, e);
		}
	}

	public List<Table> getTables() {
		List<Table> result = new ArrayList<>();

		try (Stream<Path> files = Files.find(properties.getDir(), 1, (path, attributes) -> {
			String filename = path.getFileName().toString().toLowerCase();
			return filename.startsWith("table_")
					&& (filename.endsWith(".csv") || filename.endsWith(".xls") || filename.endsWith(".xlsx"));
		})) {
			files.forEach(file -> {
				String name = file.getFileName().toString().toLowerCase()
						.replaceAll("^table_", "")
						.replaceAll("\\.[a-z]+$", "");
				Table table = new Table(name, file.getFileName().toString());

				try (FileInputStream inputStream = new FileInputStream(file.toFile());
						Stream<Row> rowStream = Windmill.parse(FileSource.of(inputStream))) {
					handleFile(table, rowStream);
					result.add(table);
				} catch (IOException e) {
					throw new IllegalStateException("could not read table file " + file);
				}
			});
		} catch (IOException e) {
			throw new IllegalStateException("could not read table files from " + properties.getDir(), e);
		}

		return result;
	}

	public Table getTable(String tableName) {
		if (StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("no table name given");
		}
		return getTables().stream()
			.filter(t -> t.getName().equalsIgnoreCase(tableName))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("table " + tableName + " not found"));
	}

	private static void handleFile(Table table, Stream<Row> rowStream) {
		final List<String> headers = new ArrayList<>();
		rowStream.forEach(row -> {
			if (row.rowIndex() == 0) {
				row.forEach(cell -> {
					String cellContent = cell.asString();
					headers.add(StringUtils.isNotBlank(cellContent)
							? cellContent.trim().toLowerCase()
							: null);
				});
			} else {
				TableRow tableRow = new TableRow();
				row.forEach(cell -> {
					String header = headers.size() > cell.columnIndex()
							? headers.get(cell.columnIndex())
							: null;
					if (StringUtils.isNotBlank(header)) {
						tableRow.put(header, cell.asString());
					}
				});
				table.add(tableRow);
			}
		});
	}

	public boolean write(Table table) {
		Path tableFilePath = properties.getDir().resolve(table.getFilename());

		ExportHeaderMapping<TableRow> headerMapping = new ExportHeaderMapping<>();
		for (String columnName : table.getColumnNames()) {
			headerMapping.add(columnName, tr -> tr.get(columnName));
		}

		ExportRowsConfig<TableRow> exportConfig = Windmill
			.export(table.getRows())
			.withHeaderMapping(headerMapping);
		String fileEnding = table.getFilename().toLowerCase().replaceAll("^.*\\.", "");
		try (FileOutputStream outStream = new FileOutputStream(tableFilePath.toFile())) {
            switch (fileEnding) {
                case "xls" -> exportConfig.asExcel(ExportExcelConfig.newXlsFile().build()).writeTo(outStream);
                case "xlsx" -> exportConfig.asExcel(ExportExcelConfig.newXlsxFile().build()).writeTo(outStream);
                case "csv" -> exportConfig.asCsv(ExportCsvConfig.builder().build()).writeTo(outStream);
                default -> throw new IllegalArgumentException();
            }
			log.debug("wrote table {} to file {}", table.getName(), table.getFilename());
			return true;
		} catch (IOException ioe) {
			log.warn("could not write table {} to file {}", table.getName(), table.getFilename(), ioe);
			return false;
		}
	}

	private String readStoredMonth(YearMonth yearMonth) {
		String filename = YEAR_MONTH_FORMAT.format(yearMonth) + ".json";
		try {
			return Files.readString(properties.getDir().resolve(DATA_SUBDIR).resolve(filename));
		} catch (IOException e) {
			throw new IllegalStateException("could not read " + filename + " from " + properties.getDir() + "/" + DATA_SUBDIR, e);
		}
	}

	private void deleteStoredMonth(YearMonth yearMonth) {
		String filename = YEAR_MONTH_FORMAT.format(yearMonth) + ".json";
		try {
			Files.delete(properties.getDir().resolve(DATA_SUBDIR).resolve(filename));
		} catch (IOException e) {
			throw new IllegalStateException("could not delete " + filename + " from " + properties.getDir() + "/" + DATA_SUBDIR, e);
		}
	}

	private void writeStoredMonth(YearMonth yearMonth, RulesResult rulesResult) {
		String filename = YEAR_MONTH_FORMAT.format(yearMonth) + ".json";
		try {
			Files.writeString(properties.getDir().resolve(DATA_SUBDIR).resolve(filename), gson.toJson(rulesResult));
		} catch (IOException e) {
			throw new IllegalStateException("could not write to " + filename + " in " + properties.getDir() + "/" + DATA_SUBDIR, e);
		}
	}

	public SortedSet<YearMonth> getStoredMonths() {
		try (Stream<Path> files = Files.find(properties.getDir().resolve(DATA_SUBDIR), 1, (p, a) -> JSON_FILE_NAME.matcher(p.getFileName().toString())
			.matches())) {
			return files
				.map(p -> YearMonth.parse(JSON_FILE_EXTENSION.matcher(p.getFileName().toString()).replaceAll(""), YEAR_MONTH_FORMAT))
				.collect(Collectors.toCollection(TreeSet::new));
		} catch (IOException e) {
			throw new IllegalStateException("could not list files from " + properties.getDir() + "/" + DATA_SUBDIR, e);
		}
	}

	public RulesResult getStoredMonth(YearMonth yearMonth) {
		return gson.fromJson(readStoredMonth(yearMonth), RulesResult.class);
	}

	public void setStoredMonth(YearMonth yearMonth, RulesResult ruleResults) {
		if (ruleResults == null) {
			deleteStoredMonth(yearMonth);
		} else {
			writeStoredMonth(yearMonth, ruleResults);
		}
	}
}
