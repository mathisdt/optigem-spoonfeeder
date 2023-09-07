package org.zephyrsoft.optigemspoonfeeder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.model.SearchableString;
import org.zephyrsoft.optigemspoonfeeder.model.Table;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.RequiredArgsConstructor;
import net.bzzt.swift.mt940.Mt940Entry;
import net.bzzt.swift.mt940.Mt940File;

@Service
@RequiredArgsConstructor
public class RuleService {
	private final PersistenceService persistenceService;

	public RulesResult apply(Mt940File input) {
		List<String> rules = persistenceService.getRules();
		List<Table> tables = persistenceService.getTables();

		Binding sharedData = new Binding();
		GroovyShell shell = new GroovyShell(sharedData);

		for (Table table : tables) {
			sharedData.setProperty(table.getName(), table);
		}

		List<Buchung> converted = new ArrayList<>();
		List<Mt940Entry> rejected = new ArrayList<>();
		for (Mt940Entry entry : input.getEntries()) {
			sharedData.setProperty("eigenkonto", new SearchableString(entry.getKontobezeichnung()));
			sharedData.setProperty("datum", entry.getValutaDatum());
			sharedData.setProperty("soll", entry.isDebit());
			sharedData.setProperty("haben", entry.isCredit());
			sharedData.setProperty("betrag", entry.getBetrag());
			sharedData.setProperty("buchungstext", new SearchableString(entry.getBuchungstext()));
			sharedData.setProperty("verwendungszweck", new SearchableString(entry.getVerwendungszweck()));
			sharedData.setProperty("bank", new SearchableString(entry.getBankKennung()));
			sharedData.setProperty("konto", new SearchableString(entry.getKontoNummer()));
			sharedData.setProperty("name", new SearchableString(entry.getName()));

			sharedData.setProperty("done", false);

			Buchung booking = null;
			for (String rule : rules) {
				shell.evaluate("import org.zephyrsoft.optigemspoonfeeder.model.*\n" + rule);

				if ((boolean) sharedData.getProperty("done")) {
					if (sharedData.getProperty("buchung") == null) {
						throw new IllegalStateException("marked as done, but no posting present");
					}

					booking = (Buchung) sharedData.getProperty("buchung");
					break;
				}
			}
			if (booking != null) {
				// fill general data
				booking.setDatum(entry.getValutaDatum());
				booking.setIncoming(entry.isCredit());
				booking.setBetrag(entry.getBetrag());

				converted.add(booking);
			} else {
				rejected.add(entry);
			}
		}
		return new RulesResult(converted, rejected);
	}
}
