package org.zephyrsoft.optigemspoonfeeder.model;

import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class RuleResult {

	private Mt940Entry input;
	private Buchung result;

	public void clearBuchung() {
		result = null;
	}

	public boolean hasBuchung() {
		return result != null;
	}
}
