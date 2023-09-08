package org.zephyrsoft.optigemspoonfeeder.model;

import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Entry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RuleResult {

	private Mt940Entry input;
	private Buchung result;
}
