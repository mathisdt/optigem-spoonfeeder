package org.zephyrsoft.optigemspoonfeeder.model;

public class SearchableString {
	private final String value;
	private final String valueLowercase;

	public SearchableString(String value) {
		this.value = value;
		valueLowercase = value == null ? null : value.toLowerCase();
	}

	public boolean contains(String... terms) {
		if (valueLowercase == null) {
			return false;
		}
		for (String term : terms) {
			if (valueLowercase.contains(term.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return value;
	}
}
