package org.zephyrsoft.optigemspoonfeeder.model;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

		Set<String> termsToApply = Arrays.stream(terms)
				.map(String::toLowerCase)
				.collect(toSet());

		// MT940 file and Hibiscus Server import use different Umlaut strategies,
		// so check both
		Set<String> additionalTermsToApply = new HashSet<>();
		for (String term : termsToApply) {
			if (term.contains("ae") || term.contains("oe") || term.contains("ue")) {
				additionalTermsToApply.add(term.replace("ae", "ä").replace("oe", "ö").replace("ue", "ü"));
			}
		}
		termsToApply.addAll(additionalTermsToApply);

		for (String term : termsToApply) {
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
