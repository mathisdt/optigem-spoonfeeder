package org.zephyrsoft.optigemspoonfeeder.service;

import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.BuchungForExport;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;

import com.coreoz.windmill.Windmill;
import com.coreoz.windmill.exports.config.ExportHeaderMapping;
import com.helger.commons.io.stream.StringInputStream;

import lombok.RequiredArgsConstructor;

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
						.add("SOLLUPROJ", b -> 0)
						.add("HabenHK", b -> b.getHabenHK())
						.add("HabenUK", b -> b.getHabenUK())
						.add("HabenProj", b -> b.getHabenProj())
						.add("HABENUPROJ", b -> 0)
						.add("Betrag", b -> b.getBetrag())
						.add("BuchText", b -> b.getBuchText())
						.add("Belegart", b -> null)
						.add("BelegNr", b -> null)
						.add("SpendeDat", b -> null)
						.add("ESP", b -> false)
						.add("DB", b -> false)
						.add("NichtSZB", b -> false)
						.add("AktionsKz", b -> null)
						.add("Stat_Land", b -> null)
						.add("Stat_LandNr", b -> null)
						.add("Stat_PLZ", b -> null)
						.add("ErfDat", b -> today)
						.add("TEXTSCHLUESSEL", b -> null)
						.add("Nummer", b -> null)
						.add("StProzent", b -> 0)
						.add("NettoBuch", b -> 1))
				.asExcel()
				.writeTo(outStream);

		return new ByteArrayInputStream(outStream.toByteArray());
	}

	public InputStream createMt940Export(List<RuleResult> complete) {
		return new StringInputStream(complete.stream()
				.filter(rr -> !rr.hasBuchung())
				.map(rr -> rr.getInput().getOriginalTextComplete())
				.collect(joining("\n-\n", "", "\n-\n")), StandardCharsets.UTF_8);
	}

}
