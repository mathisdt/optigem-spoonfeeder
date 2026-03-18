package org.zephyrsoft.optigemspoonfeeder.source.parser;

import java.io.FileReader;
import java.io.LineNumberReader;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;

import static org.assertj.core.api.Assertions.assertThat;

class Mt940ParserTest {
    @Test
    void valutaVsDate() throws Exception {
        FileReader fileReader = new FileReader("src/test/resources/mt940/valuta-vs-date.sta");
        SourceFile parsed = Mt940Parser.parse(new LineNumberReader(fileReader));

        Condition<SourceEntry> valutaInPreviousYear = new Condition<>(se -> se.getValutaDatum() != null && se.getDatum() != null && se.getValutaDatum().getYear() < se.getDatum().getYear(),
            "valuta date in previous year compared with posting date");
        Condition<SourceEntry> valutaSameAsDate = new Condition<>(se -> se.getValutaDatum() != null && se.getDatum() != null && se.getValutaDatum().getYear() == se.getDatum().getYear(),
            "valuta date in same year as posting date");
        Condition<SourceEntry> postingDateNotPresent = new Condition<>(se -> se.getValutaDatum() != null && se.getDatum() == null,
            "valuta date present, but no posting date");
        assertThat(parsed.getEntries())
            .hasSize(7)
            .areExactly(3, valutaInPreviousYear)
            .areExactly(3, valutaSameAsDate)
            .areExactly(1, postingDateNotPresent);
    }
}
