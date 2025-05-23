package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import lombok.Getter;

@Getter
public class Table {
	private static final Comparator<String> COMPARE_AS_NUMBERS_IF_POSSIBLE = (s1, s2) -> {
		if (NumberUtils.isDigits(s1) && NumberUtils.isDigits(s2)) {
			return Comparator.<Integer>naturalOrder().compare(Integer.valueOf(s1), Integer.valueOf(s2));
		} else {
			return Comparator.<String>naturalOrder().compare(s1, s2);
		}
	};

	private final String name;
	private final String filename;
	private final List<TableRow> rows = new ArrayList<>();

	public Table(String name, String filename) {
		this.name = name;
		this.filename = filename;
	}

	public void add(TableRow e) {
		rows.add(e);
	}

	public int size() {
		return rows.size();
	}

	public boolean contains(String column, String value) {
		return rows.stream()
				.anyMatch(r -> r.get(column) != null && r.get(column).equalsIgnoreCase(value));
	}

	public boolean contains(String column, SearchableString value) {
		return rows.stream()
				.anyMatch(r -> r.get(column) != null && r.get(column).equalsIgnoreCase(value.toString()));
	}

	public TableRow where(String column, String value) {
		return rows.stream()
				.filter(r -> r.get(column) != null && r.get(column).equalsIgnoreCase(value))
				.findFirst()
				.orElse(null);
	}

	public TableRow where(String column, SearchableString value) {
		return rows.stream()
				.filter(r -> r.get(column) != null && r.get(column).equalsIgnoreCase(value.toString()))
				.findFirst()
				.orElse(null);
	}

	public Set<String> getColumnNames() {
		Set<String> columnNames = new HashSet<>();
		for (TableRow row : rows) {
			columnNames.addAll(row.keys());
		}
		return columnNames;
	}

	/**
	 * 1. non-number values are simply ignored<br/>
	 * 2. if no values are present at all, 0 is returned
	 */
	public int max(String columnName) {
		return rows.stream()
			.mapToInt(r -> asNumber(r.get(columnName)))
			.filter(i -> i >= 0)
			.max()
			.orElse(0);
	}

	private static int asNumber(String str) {
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return -1;
		}
	}

	public void sortBy(String... columnNames) {
		Comparator<TableRow> comp = Comparator.comparing(r -> r.get(columnNames[0]), Comparator.nullsFirst(COMPARE_AS_NUMBERS_IF_POSSIBLE));
		for (int i = 1; i < columnNames.length; i++) {
			int index = i;
			comp = comp.thenComparing(r -> r.get(columnNames[index]), Comparator.nullsFirst(COMPARE_AS_NUMBERS_IF_POSSIBLE));
		}
		rows.sort(comp);
	}

	@Override
	public String toString() {
		return "Table [name=" + name + ", containing " + rows.size() + " rows]";
	}
}
