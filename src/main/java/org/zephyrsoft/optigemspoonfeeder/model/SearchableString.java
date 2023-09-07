package org.zephyrsoft.optigemspoonfeeder.model;

public class SearchableString {
	private final String value;
	private final String valueLowercase;

	public SearchableString(String value) {
		this.value = value;
		valueLowercase = value.toLowerCase();
	}

	public boolean contains(String... terms) {
		for (String term : terms) {
			if (valueLowercase.contains(term)) {
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
