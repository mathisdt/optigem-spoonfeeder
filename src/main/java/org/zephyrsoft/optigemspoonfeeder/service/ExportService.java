package org.zephyrsoft.optigemspoonfeeder.service;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.BuchungForExport;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;

import com.coreoz.windmill.Windmill;
import com.coreoz.windmill.exports.config.ExportHeaderMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
	private final OptigemSpoonfeederProperties properties;

	public InputStream createBuchungenExport(List<RuleResult> complete) {
		List<BuchungForExport> buchungen = complete.stream()
				.filter(RuleResult::hasBuchung)
				.flatMap(rr -> rr.getResult().stream())
				.map(b -> new BuchungForExport(b, properties))
				.toList();

		final LocalDate today = LocalDate.now();

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		Windmill.export(buchungen)
				.withHeaderMapping(new ExportHeaderMapping<BuchungForExport>()
						.add("Datum", BuchungForExport::getDatum)
						.add("SollHK", BuchungForExport::getSollHK)
						.add("SollUK", BuchungForExport::getSollUK)
						.add("SollProj", BuchungForExport::getSollProj)
						.add("HabenHK", BuchungForExport::getHabenHK)
						.add("HabenUK", BuchungForExport::getHabenUK)
						.add("HabenProj", BuchungForExport::getHabenProj)
						.add("Betrag", BuchungForExport::getBetrag)
						.add("BuchText", ExportService::buchText)
						.add("ESP", b -> 0)
						.add("DB", b -> 0)
						.add("ErfDat", b -> today)
						.add("StProzent", b -> 0)
						.add("NettoBuch", b -> 0)
                        .add("Belegart", b -> "")
                        .add("BelegNr", b -> 0))
				.asExcel()
				.writeTo(outStream);

		return new ByteArrayInputStream(outStream.toByteArray());
	}

	private static String buchText(BuchungForExport b) {
		if (StringUtils.isEmpty(b.getBuchText())) {
			// may not be empty
			return " ";
		} else if (b.getBuchText().length() > 52) {
			log.info("truncated BuchText on {} ({}): {}", b.getDatum(), b.getBetrag(), b.getBuchText());
			return b.getBuchText().substring(0, 52);
		} else {
			return b.getBuchText();
		}
	}

    public static InputStream createMt940Export(List<RuleResult> complete) {
        return new ByteArrayInputStream(complete.stream()
            .filter(rr -> !rr.hasBuchung())
            .map(rr -> rr.getInput().getAsMT940())
            .collect(joining("\n-\n", "", "\n-\n"))
            .getBytes(StandardCharsets.UTF_8));
    }

}
