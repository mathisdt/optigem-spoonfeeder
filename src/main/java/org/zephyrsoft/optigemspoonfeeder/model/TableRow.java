package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class TableRow {
	private final Map<String, String> columnValues = new HashMap<>();

	public String get(String columnName) {
		if (columnName == null) {
			return null;
		}
		return columnValues.get(columnName.toLowerCase());
	}

	public void put(String columnName, String columnValue) {
		if (columnName == null) {
			throw new IllegalArgumentException("column name may not be null");
		}
		columnValues.put(columnName.toLowerCase(), columnValue);
	}

	public int size() {
		return columnValues.size();
	}
}
