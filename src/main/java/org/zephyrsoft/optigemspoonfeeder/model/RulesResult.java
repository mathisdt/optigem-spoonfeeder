package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class RulesResult {
	private List<RuleResult> results;
	private String logMessages;

	public int size() {
		return results.size();
	}

	public Stream<RuleResult> stream() {
		return results.stream();
	}

}
