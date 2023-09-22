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
import com.helger.commons.io.stream.StringInputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
	private final OptigemSpoonfeederProperties properties;

	public InputStream createBuchungenExport(List<RuleResult> complete) {
		List<BuchungForExport> buchungen = complete.stream()
				.filter(rr -> rr.hasBuchung())
				.map(rr -> new BuchungForExport(rr.getResult(), properties))
				.toList();

		final LocalDate today = LocalDate.now();

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		Windmill.export(buchungen)
				.withHeaderMapping(new ExportHeaderMapping<BuchungForExport>()
						.add("Datum", b -> b.getDatum())
						.add("SollHK", b -> b.getSollHK())
						.add("SollUK", b -> b.getSollUK())
						.add("SollProj", b -> b.getSollProj())
						.add("HabenHK", b -> b.getHabenHK())
						.add("HabenUK", b -> b.getHabenUK())
						.add("HabenProj", b -> b.getHabenProj())
						.add("Betrag", b -> b.getBetrag())
						.add("BuchText", b -> buchText(b))
						.add("ESP", b -> 0)
						.add("DB", b -> 0)
						.add("ErfDat", b -> today)
						.add("StProzent", b -> 0)
						.add("NettoBuch", b -> 0))
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

	public InputStream createMt940Export(List<RuleResult> complete) {
		return new StringInputStream(complete.stream()
				.filter(rr -> !rr.hasBuchung())
				.map(rr -> rr.getInput().getAsMT940())
				.collect(joining("\n-\n", "", "\n-\n")), StandardCharsets.UTF_8);
	}

}
