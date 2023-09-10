package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.Comparator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Konto implements Comparable<Konto> {
	// compare IBAN without country and checksum
	private static final Comparator<Konto> COMPARATOR = Comparator.comparing(k -> k.getIban().substring(4));

	private final String bezeichnung;
	private final String iban;
	private final String id;

	@Override
	public int compareTo(Konto o) {
		return COMPARATOR.compare(this, o);
	}
}
