package org.zephyrsoft.optigemspoonfeeder.model;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class RuleResult {

	private SourceEntry input;
	private Buchung result;

	public void clearBuchung() {
		result = null;
	}

	public boolean hasBuchung() {
		return result != null;
	}

	public void fillGeneralData() {
		if (result != null) {
			// fill general data
			result.setDatum(input.getValutaDatum());
			result.setIncoming(input.isCredit());
			result.setBetrag(input.getBetrag());
			if (StringUtils.isNotBlank(input.getVerwendungszweckClean())) {
				result.setBuchungstext(StringUtils.isNotBlank(result.getBuchungstext())
					? result.getBuchungstext() + " - " + input.getVerwendungszweckClean().trim()
					: input.getVerwendungszweckClean().trim());
			}
		}
	}
}
