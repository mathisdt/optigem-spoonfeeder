package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.bzzt.swift.mt940.Mt940Entry;

@Getter
@AllArgsConstructor
public class RulesResult {
	private final List<Buchung> converted;
	private final List<Mt940Entry> rejected;
}
