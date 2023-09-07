package org.zephyrsoft.optigemspoonfeeder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zephyrsoft.optigemspoonfeeder.model.Table;

@SpringBootTest(properties = { "org.zephyrsoft.optigem-spoonfeeder.dir=src/test/resources/basedata" })
class PersistenceServiceIT {

	@Autowired
	PersistenceService service;

	@Test
	void getTables() {
		List<Table> tables = service.getTables();
		assertNotNull(tables);
		assertEquals(1, tables.size());
		assertEquals(3, tables.get(0).size());

		assertEquals(7, tables.get(0).getRows().get(0).size());
		assertEquals(5, tables.get(0).getRows().get(1).size());
		assertEquals(3, tables.get(0).getRows().get(2).size());

		assertEquals("3", tables.get(0).getRows().get(2).get("Nr"));
		assertEquals("Nachname Test 3", tables.get(0).getRows().get(2).get("nachname"));
		assertEquals("IBAN-Test0000000000003", tables.get(0).getRows().get(2).get("IBAN"));
	}

	@Test
	void getRules() {
		List<String> rules = service.getRules();
		assertNotNull(rules);
		assertEquals(4, rules.size());
	}

}
