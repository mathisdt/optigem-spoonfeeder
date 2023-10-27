package org.zephyrsoft.optigemspoonfeeder.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SourceEntryTest {

	@Test
	void getMt940VerwendungszweckString() {
		SourceEntry entry = new SourceEntry();

		entry.setVerwendungszweck("");
		assertEquals("", entry.getMt940VerwendungszweckString());

		entry.setVerwendungszweck("short");
		assertEquals("?20short", entry.getMt940VerwendungszweckString());

		entry.setVerwendungszweck("this has more than 27 characters");
		assertEquals("?20this has more than 27 chara?21cters", entry.getMt940VerwendungszweckString());

		entry.setVerwendungszweck("this message is even longer and spans over more than two times 27 characters");
		assertEquals("?20this message is even longer?21 and spans over more than t?22wo times 27 characters",
				entry.getMt940VerwendungszweckString());
	}

}
