package org.zephyrsoft.optigemspoonfeeder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.SearchableString;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940Entry;
import org.zephyrsoft.optigemspoonfeeder.mt940.Mt940File;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleService {
	private final PersistenceService persistenceService;

	public List<RuleResult> apply(Mt940File input) {
		String rules = persistenceService.getRules();
		List<Table> tables = persistenceService.getTables();

		Binding sharedData = new Binding();
		GroovyShell shell = new GroovyShell(sharedData);

		for (Table table : tables) {
			sharedData.setProperty(table.getName(), table);
		}

		List<RuleResult> result = new ArrayList<>();
		for (Mt940Entry entry : input.getEntries()) {
			sharedData.setProperty("eigenkonto", new SearchableString(entry.getKontobezeichnung()));
			sharedData.setProperty("datum", entry.getValutaDatum());
			sharedData.setProperty("soll", entry.isDebit());
			sharedData.setProperty("haben", entry.isCredit());
			sharedData.setProperty("betrag", entry.getBetrag());
			sharedData.setProperty("buchungstext", new SearchableString(entry.getBuchungstext()));
			sharedData.setProperty("verwendungszweck", new SearchableString(entry.getVerwendungszweckClean()));
			sharedData.setProperty("bank", new SearchableString(entry.getBankKennung()));
			sharedData.setProperty("konto", new SearchableString(entry.getKontoNummer()));
			sharedData.setProperty("name", new SearchableString(entry.getName()));

			Buchung booking = (Buchung) shell.evaluate("import org.zephyrsoft.optigemspoonfeeder.model.*\n" + rules);

			if (booking != null) {
				// fill general data
				booking.setDatum(entry.getValutaDatum());
				booking.setIncoming(entry.isCredit());
				booking.setBetrag(entry.getBetrag());
			}
			result.add(new RuleResult(entry, booking));
		}
		return result;
	}
}
