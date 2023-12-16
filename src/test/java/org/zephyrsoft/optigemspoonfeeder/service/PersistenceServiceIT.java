package org.zephyrsoft.optigemspoonfeeder.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = { "org.zephyrsoft.optigem-spoonfeeder.dir=src/test/resources/basedata" })
class PersistenceServiceIT {

	@Autowired
	private PersistenceService service;
	@Autowired
	private OptigemSpoonfeederProperties properties;

	@Test
	void getTables() {
		List<Table> tables = service.getTables();
		assertNotNull(tables);
		assertThat(tables.size()).isGreaterThanOrEqualTo(1);
		Table personen = tables.stream()
			.filter(t -> t.getName().equals("personen"))
			.findAny()
			.orElseThrow();
		assertEquals(3, personen.size());

		assertEquals(7, personen.getRows().get(0).size());
		assertEquals(5, personen.getRows().get(1).size());
		assertEquals(3, personen.getRows().get(2).size());

		assertEquals("3", personen.getRows().get(2).get("Nr"));
		assertEquals("Nachname Test 3", personen.getRows().get(2).get("nachname"));
		assertEquals("IBAN-Test0000000000003", personen.getRows().get(2).get("IBAN"));
	}

	@Test
	void getRules() {
		String rules = service.getRules();
		assertFalse(StringUtils.isBlank(rules));
	}

	@Test
	void writeTable() throws IOException {
		Files.copy(properties.getDir().resolve("table_personen.xlsx"),
			properties.getDir().resolve("table_tmp.xlsx"),
			StandardCopyOption.REPLACE_EXISTING);
		Table tmp = service.getTables().stream()
			.filter(t -> t.getName().equals("tmp"))
			.findAny()
			.orElseThrow();
		assertEquals(3, tmp.size());

		tmp.add(new TableRow()
			.with("Nr", String.valueOf(tmp.max("Nr") + 1))
			.with("Nachname", "Integration Test 1a")
			.with("Vorname", "Integration Test 1b")
			.with("IBAN", "DE1234567890123456789012"));
		service.write(tmp);

		Table tmpAfterUpdate = service.getTables().stream()
			.filter(t -> t.getName().equals("tmp"))
			.findAny()
			.orElseThrow();
		assertEquals(4, tmpAfterUpdate.size());
	}

}
