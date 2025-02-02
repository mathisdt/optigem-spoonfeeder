package org.zephyrsoft.optigemspoonfeeder.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE) // for GSON
public class RuleResult {

	private SourceEntry input;
	private final List<Buchung> result = new ArrayList<>();

	public RuleResult(final SourceEntry input, Buchung buchung) {
		this.input = input;
		if (buchung != null) {
			result.add(buchung);
		}
	}

	public void clearBuchungen() {
		result.clear();
	}

	public boolean hasBuchung() {
		return !result.isEmpty();
	}

	public boolean hasBuchungenForWholeSum() {
		return !result.isEmpty() && Objects.compare(input.getBetrag(),
			result.stream()
				.filter(b -> !b.isEmpty())
				.map(Buchung::getBetrag)
				.filter(b -> b != null && b.compareTo(BigDecimal.ZERO) > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add),
			BigDecimal::compareTo) == 0;
	}

	public RuleResult withBuchung(Buchung buchung) {
		return new RuleResult(input, buchung);
	}

	public void fillGeneralData() {
		for (Buchung buchung : result) {
			// fill general data
			buchung.setDatum(input.getValutaDatum());
			buchung.setIncoming(input.isCredit());
			if (buchung.getBetrag() == null) {
				buchung.setBetrag(input.getBetrag());
			}
			if (StringUtils.isBlank(buchung.getBuchungstext()) && StringUtils.isNotBlank(input.getVerwendungszweckClean())) {
				buchung.setBuchungstext(input.getVerwendungszweckClean().trim());
			}
		}
	}
}
