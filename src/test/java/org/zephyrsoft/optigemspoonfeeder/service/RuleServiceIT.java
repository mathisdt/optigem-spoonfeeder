package org.zephyrsoft.optigemspoonfeeder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;
import org.zephyrsoft.optigemspoonfeeder.source.parser.Mt940Parser;

@SpringBootTest(properties = { "org.zephyrsoft.optigem-spoonfeeder.dir=src/test/resources/basedata" })
class RuleServiceIT {

	@Autowired
	RuleService service;

	@Test
	void apply() throws Exception {
		try (FileReader fileReader = new FileReader("src/test/resources/mt940/example.sta")) {
			SourceFile input = Mt940Parser.parse(new LineNumberReader(fileReader));
			RulesResult result = service.apply(input);

			assertNotNull(result);

			List<RuleResult> rulesResult = result.getResults();
			assertNotNull(rulesResult);
			assertEquals(8, rulesResult.size());

			// matched:
			assertThat(rulesResult)
					.areExactly(1, matches(4940, 0, 0, "Telekom"));
			assertThat(rulesResult)
					.areExactly(1, matches(8010, 2, 0, "Vorname Test 2 Nachname Test 2"));
			assertThat(rulesResult)
					.areExactly(1, matches(8010, 1, 1, "Vorname Test 1 Nachname Test 1"));
			assertThat(rulesResult)
					.areExactly(1, matches(8205, 4, 21, "GFYC-Freizeit"));
			assertThat(rulesResult)
					.areExactly(1, matches(8205, 5, 20, "Roots-Freizeit"));
			assertThat(rulesResult)
					.areExactly(1, matches(8205, 5, 20, "Freizeit Junge Erwachsene"));
			assertThat(rulesResult)
					.areExactly(1, matches(1360, 0, 0, "Bareinzahlung"));

			// unmatched:
			assertThat(rulesResult)
					.areExactly(1, new Condition<>(rr -> rr.getResult().isEmpty()
							&& rr.getInput().getVerwendungszweck().contains("Einzahlung Bar"), ""));
		}
	}

	private static Condition<RuleResult> matches(int hk, int uk, int p, String text) {
		return new Condition<>(rr -> rr.getResult() != null
				&& rr.getResult().size() == 1
				&& rr.getResult().get(0).getHauptkonto() == hk
				&& rr.getResult().get(0).getUnterkonto() == uk
				&& rr.getResult().get(0).getProjekt() == p
				&& Objects.equals(rr.getResult().get(0).getBuchungstext(), text), "");
	}
}
