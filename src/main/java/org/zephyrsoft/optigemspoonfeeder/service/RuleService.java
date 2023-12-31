package org.zephyrsoft.optigemspoonfeeder.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.model.Buchung;
import org.zephyrsoft.optigemspoonfeeder.model.RuleResult;
import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;
import org.zephyrsoft.optigemspoonfeeder.model.SearchableString;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleService {
	private static class LogWrapper {
		private final StringBuilder memory = new StringBuilder();

		@SuppressWarnings("unused") // used in Groovy script
		public void log(String msg, Object[] args) {
			log.info(msg, args);
			if (!memory.isEmpty()) {
				memory.append("\n");
			}
			memory.append(MessageFormatter.arrayFormat(msg, args).getMessage());
		}

		public String getComplete() {
			return memory.toString();
		}
	}

	private final PersistenceService persistenceService;

	public RulesResult apply(SourceFile input) {
		String rules = persistenceService.getRules();
		List<Table> tables = persistenceService.getTables();

		Binding sharedData = new Binding();
		GroovyShell shell = new GroovyShell(sharedData);
		Script parsed = shell.parse("import org.zephyrsoft.optigemspoonfeeder.model.*\n"
				+ "def log(String msg, Object... args) {\n"
				+ "  logWrapper.log(msg, args)\n"
				+ "}\n"
				+ "static def buchung(Object hauptkonto, Object unterkonto = null, Object projekt = null, Object buchungstext = null) {\n"
				+ "  return new Buchung(hauptkonto, unterkonto, projekt, buchungstext)\n"
				+ "}\n"
				+ rules);

		for (Table table : tables) {
			sharedData.setProperty(table.getName(), table);
		}

		List<RuleResult> result = new ArrayList<>();
		LogWrapper logWrapper = new LogWrapper();
		sharedData.setProperty("logWrapper", logWrapper);
		for (SourceEntry entry : input.getEntries()) {
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

			Buchung booking = (Buchung) parsed.run();

			RuleResult ruleResult = new RuleResult(entry, booking);
			ruleResult.fillGeneralData();
			result.add(ruleResult);
		}
		return new RulesResult(result, logWrapper.getComplete());
	}
}
