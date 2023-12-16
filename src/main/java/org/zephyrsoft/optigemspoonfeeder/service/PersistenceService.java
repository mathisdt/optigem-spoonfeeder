package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;

import com.coreoz.windmill.Windmill;
import com.coreoz.windmill.exports.config.ExportHeaderMapping;
import com.coreoz.windmill.exports.config.ExportRowsConfig;
import com.coreoz.windmill.exports.exporters.csv.ExportCsvConfig;
import com.coreoz.windmill.exports.exporters.excel.ExportExcelConfig;
import com.coreoz.windmill.files.FileSource;
import com.coreoz.windmill.imports.Row;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

	private static final String RULES_FILENAME = "rules.groovy";

	private final OptigemSpoonfeederProperties properties;

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
}
