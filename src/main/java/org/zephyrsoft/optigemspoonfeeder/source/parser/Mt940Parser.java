/*
 * Copyright (C) 2008 Arnout Engelen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Origin: https://github.com/ccavanaugh/jgnash
 */
package org.zephyrsoft.optigemspoonfeeder.source.parser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry;
import org.zephyrsoft.optigemspoonfeeder.source.SourceEntry.SollHabenKennung;
import org.zephyrsoft.optigemspoonfeeder.source.SourceFile;
import org.zephyrsoft.optigemspoonfeeder.source.SourceRecord;

/**
 * MT940 Parser.
 *
 * @author Arnout Engelen
 * @author Miroslav Holubec
 * @author Craig Cavanaugh
 */
public class Mt940Parser {

	private static final String PREFIX_KONTOBEZEICHNUNG = ":25:";
	private static final String PREFIX_ENTRY_START = ":61:";
	private static final String PREFIX_MEHRZWECKFELD = ":86:";

	// see https://www.kontopruef.de/mt940s.shtml for field key explanation
	private static final String FIELD_KEY_BUCHUNGSTEXT = "00";
	private static final Set<String> FIELD_KEYS_VERWENDUNGSZWECK = Set.of("20", "21", "22", "23", "24", "25", "26",
			"27", "28", "29", "60", "61", "62", "63");
	private static final String FIELD_KEY_BANKKENNUNG = "30";
	private static final String FIELD_KEY_KONTONUMMER = "31";
	private static final Set<String> FIELD_KEYS_NAME = Set.of("32", "33");

	/**
	 * Parse the Mt940-file. Mt940 records are delimited by '-'.
	 *
	 * @param reader reader
	 * @return SourceFile instance
	 * @throws IOException            An IO exception occurred
	 * @throws DateTimeParseException parse error occurred reading text
	 */
	public static SourceFile parse(final LineNumberReader reader) throws IOException, DateTimeParseException {
		final SourceFile mt940File = new SourceFile();

		List<String> recordLines = new ArrayList<>();

		String currentLine = reader.readLine();
		while (currentLine != null) {
			if (currentLine.startsWith("-")) {
				// Parse this record and add it to the file
				mt940File.getRecords().add(parseRecord(recordLines));

				// Clear recordLines to start on the next record
				recordLines = new ArrayList<>();
			} else {
				recordLines.add(currentLine);
			}
			currentLine = reader.readLine();
		}

		// A file might not end with a trailing '-' (e.g. from Rabobank):
		mt940File.getRecords().add(parseRecord(recordLines));

		return mt940File;
	}

	/**
	 * An mt940-record first has a couple of 'header' lines that do not start with a
	 * ':'.
	 * <p>
	 * After that, a line that doesn't start with a ':' is assumed to belong to the
	 * previous 'real' line.
	 *
	 * @param recordLines list of records
	 * @return List of strings that have been correctly merged
	 */
	private static List<String> mergeLines(final List<String> recordLines) {
		List<String> retVal = new ArrayList<>();

		StringBuilder currentString = new StringBuilder();

		boolean inMessage = false;
		for (String string : recordLines) {
			// Are we in the message proper, after the
			// header?
			if (inMessage) {
				if (string.startsWith(":")) {
					retVal.add(currentString.toString());
					currentString = new StringBuilder();
				}
				currentString.append(string);
			} else {
				if (string.startsWith(":")) {
					// then we're past the header
					inMessage = true;
					currentString = new StringBuilder(string);
				} else {
					// add a line of the header
					retVal.add(string);
				}
			}
		}
		return retVal;
	}

	/**
	 * An mt940-record consists of some general lines and a couple of entries
	 * consisting of a :61: and a :86:-section.
	 *
	 * @param recordLines the List of MT940 records to parse
	 * @return and generate Mt940 Record
	 * @throws DateTimeParseException parse error occurred reading text
	 */
	private static SourceRecord parseRecord(final List<String> recordLines) throws DateTimeParseException {
		SourceRecord mt940Record = new SourceRecord();

		// Merge 'lines' that span multiple actual lines.
		List<String> mergedLines = mergeLines(recordLines);

		SourceEntry currentEntry = null;
		String currentAccount = null;
		final List<String> originalLines = new ArrayList<>();
		for (String line : mergedLines) {
			originalLines.add(line);
			if (line.startsWith(PREFIX_KONTOBEZEICHNUNG)) {
				currentAccount = line.substring(PREFIX_KONTOBEZEICHNUNG.length());
			}

			if (line.startsWith(PREFIX_ENTRY_START)) {
				currentEntry = nextEntry(mt940Record.getEntries(), currentEntry, currentAccount, originalLines);

				line = line.substring(PREFIX_ENTRY_START.length());
				line = parseDatumJJMMTT(currentEntry, line);
				// for now don't handle the buchungsdatum. It is optional.
				if (startsWithBuchungsDatum(line)) {
					line = line.substring(4);
				}
				// for now only support C and D, not RC and RD
				line = parseSollHabenKennung(currentEntry, line);
				line = parseBetrag(currentEntry, line);
			}
			if (line.startsWith(PREFIX_MEHRZWECKFELD) && currentEntry != null) {
				parseMehrzweckfeld(line.substring(PREFIX_MEHRZWECKFELD.length()), currentEntry);
			}
		}

		// add the last one:
		nextEntry(mt940Record.getEntries(), currentEntry, currentAccount, originalLines);

		return mt940Record;
	}

	private static void parseMehrzweckfeld(String mehrzweckfeld, SourceEntry currentEntry) {
		currentEntry.setGeschaeftsvorfallCode(mehrzweckfeld.substring(0, 3));
		String separator = mehrzweckfeld.substring(3, 4);
		String[] fields = mehrzweckfeld.substring(4).split(Pattern.quote(separator));
		for (String field : fields) {
			String key = field.substring(0, 2);
			if (key.equals(FIELD_KEY_BUCHUNGSTEXT)) {
				currentEntry.setBuchungstext(field.substring(2));
			} else if (FIELD_KEYS_VERWENDUNGSZWECK.contains(key)) {
				currentEntry.addToVerwendungszweck(field.substring(2));
			} else if (key.equals(FIELD_KEY_BANKKENNUNG)) {
				currentEntry.setBankKennung(field.substring(2));
			} else if (key.equals(FIELD_KEY_KONTONUMMER)) {
				currentEntry.setKontoNummer(field.substring(2));
			} else if (FIELD_KEYS_NAME.contains(key)) {
				currentEntry.addToName(field.substring(2));
			}
		}
	}

	/**
	 * Adds the current entry to the result as a side-effect, if available.
	 *
	 * @param entries       entry list
	 * @param previousEntry entry to add if not null;
	 * @return new working {@code SourceEntry}
	 */
	private static SourceEntry nextEntry(List<SourceEntry> entries, SourceEntry previousEntry, String currentAccount,
			List<String> originalLines) {
		if (previousEntry != null) {
			entries.add(previousEntry);
		}

		originalLines.clear();
		SourceEntry entry = new SourceEntry();
		entry.setKontobezeichnung(currentAccount);
		return entry;
	}

	/**
	 * BuchungsDatum is a 4-character optional field - but how can we check whether
	 * it was included.
	 * <p>
	 * The field is directly followed by the mandatory 'soll/haben-kennung'
	 * character, so we assume that when the string starts with a digit that's
	 * probably the buchungsdatum
	 *
	 * @param line line to check for BuchungsDatum
	 * @return true if found
	 */
	private static boolean startsWithBuchungsDatum(final String line) {
		return line != null && line.matches("^\\d.*");
	}

	/**
	 * Parse the value, put it into the entry.
	 *
	 * @param currentEntry working {@code SourceEntry}
	 * @param line         line to parse decimal value from
	 * @return the rest of the string to be parsed
	 */
	private static String parseBetrag(final SourceEntry currentEntry, final String line) {
		int endIndex = line.indexOf('N');
		if (endIndex < 0) {
			endIndex = line.indexOf('F');
		}

		String decimal = line.substring(0, endIndex);
		decimal = decimal.replace(",", ".");

		// According to the MT940 Standard the amount (field :61:) could start with the
		// last character of the currency
		// e.g. R for EUR
		// See: https://www.kontopruef.de/mt940s.shtml
		// This code removes any character which is not a decimal or point.
		decimal = decimal.replaceAll("[^\\d.]", "");

		currentEntry.setBetrag(new BigDecimal(decimal));

		return line.substring(endIndex);
	}

	/**
	 * Parse the debit/credit value, put it into the entry.
	 *
	 * @param currentEntry working {@code SourceEntry}
	 * @param string       credit / debit line to parse
	 * @return the rest of the string to be parsed
	 */
	private static String parseSollHabenKennung(final SourceEntry currentEntry, final String string) {
		String s = string;

		if (string.startsWith("D")) {
			currentEntry.setSollHabenKennung(SollHabenKennung.DEBIT);
			s = string.substring(1);
		} else if (string.startsWith("C")) {
			currentEntry.setSollHabenKennung(SollHabenKennung.CREDIT);
			s = string.substring(1);
		} else {
			throw new UnsupportedOperationException("soll-haben-kennung " + s + " not yet supported");
		}
		return s;
	}

	/**
	 * Parse the formatted date, put it into the entry.
	 *
	 * @param currentEntry working {@code SourceEntry}
	 * @param string       string to parse date from
	 * @return the rest of the string to be parsed
	 * @throws DateTimeParseException thrown if date format is bad
	 */
	private static String parseDatumJJMMTT(final SourceEntry currentEntry, final String string)
			throws DateTimeParseException {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd");

		final String date = string.substring(0, 6);

		currentEntry.setValutaDatum(LocalDate.from(dateTimeFormatter.parse(date)));

		return string.substring(6);
	}
}
