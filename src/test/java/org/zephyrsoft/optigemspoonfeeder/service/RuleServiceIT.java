package org.zephyrsoft.optigemspoonfeeder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Objects;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940File;
import org.zephyrsoft.optigemspoonfeeder.mt940.parser.Mt940Parser;

@SpringBootTest(properties = { "org.zephyrsoft.optigem-spoonfeeder.dir=src/test/resources/basedata" })
class RuleServiceIT {

	@Autowired
	RuleService service;

	@Test
	void apply() throws Exception {
		try (FileReader fileReader = new FileReader("src/test/resources/mt940/example.sta")) {
			Mt940File input = Mt940Parser.parse(new LineNumberReader(fileReader));
			RulesResult rulesResult = service.apply(input);

			assertNotNull(rulesResult);

			assertNotNull(rulesResult.getConverted());
			assertEquals(7, rulesResult.getConverted().size());
			assertThat(rulesResult.getConverted())
					.areExactly(1, matches(4940, 0, 0, null));
			assertThat(rulesResult.getConverted())
					.areExactly(1, matches(8010, 2, 0, "Spende Vorname Test 2 Nachname Test 2"));
			assertThat(rulesResult.getConverted())
					.areExactly(1, matches(8010, 1, 1, "Spende Vorname Test 1 Nachname Test 1"));
			assertThat(rulesResult.getConverted())
					.areExactly(1, matches(8205, 4, 21, null));
			assertThat(rulesResult.getConverted())
					.areExactly(2, matches(8205, 5, 20, null));
			assertThat(rulesResult.getConverted())
					.areExactly(1, matches(1360, 0, 0, null));

			assertNotNull(rulesResult.getRejected());
			assertEquals(1, rulesResult.getRejected().size());
			assertThat(rulesResult.getRejected())
					.anyMatch(e -> e.getVerwendungszweck().contains("Einzahlung Bar"));
		}
	}

	private static Condition<RuleResult> matches(int hk, int uk, int p, String text) {
		return new Condition<>(b -> b.getResult() != null
				&& b.getResult().getHauptkonto() == hk
				&& b.getResult().getUnterkonto() == uk
				&& b.getResult().getProjekt() == p
				&& Objects.equals(b.getResult().getBuchungstext(), text), "");
	}
}
