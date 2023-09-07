package org.zephyrsoft.optigemspoonfeeder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileReader;
import java.io.LineNumberReader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;

import net.bzzt.swift.mt940.Mt940File;
import net.bzzt.swift.mt940.parser.Mt940Parser;

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
			assertNotNull(rulesResult.getRejected());
			assertEquals(7, rulesResult.getConverted().size());
			assertEquals(1, rulesResult.getRejected().size());
		}
	}

}
