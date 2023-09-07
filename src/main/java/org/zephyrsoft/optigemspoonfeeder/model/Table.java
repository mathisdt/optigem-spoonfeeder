package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Table {
	private String name;
	private final List<TableRow> rows = new ArrayList<>();

	public Table(String name) {
		this.name = name;
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

	@Override
	public String toString() {
		return "Table [name=" + name + ", containing " + rows.size() + " rows]";
	}
}
