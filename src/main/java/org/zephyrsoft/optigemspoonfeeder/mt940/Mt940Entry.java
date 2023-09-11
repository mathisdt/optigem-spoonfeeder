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
package org.zephyrsoft.optigemspoonfeeder.mt940;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Mt940Entry {
	@Getter
	@AllArgsConstructor
	public enum SollHabenKennung {
		/** money was transferred to the current account */
		CREDIT("C", "051"),
		/** money was transferred away from the current account */
		DEBIT("D", "021");

		private String abbrev;
		private String gvc;
	}

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");
	private static final DateTimeFormatter DATE_NOYEAR = DateTimeFormatter.ofPattern("MMdd");
	private final NumberFormat waehrung;

	private String kontobezeichnung;

	private LocalDate valutaDatum;

	private SollHabenKennung sollHabenKennung;

	private BigDecimal betrag;

	private String geschaeftsvorfallCode;
	private String buchungstext;
	private String verwendungszweck;
	private String bankKennung;
	private String kontoNummer;
	private String name;

	public Mt940Entry() {
		waehrung = NumberFormat.getNumberInstance(Locale.GERMAN);
		waehrung.setGroupingUsed(false);
		waehrung.setMinimumFractionDigits(2);
		waehrung.setMaximumFractionDigits(2);
	}

	public boolean isCredit() {
		return sollHabenKennung == SollHabenKennung.CREDIT;
	}

	public boolean isDebit() {
		return sollHabenKennung == SollHabenKennung.DEBIT;
	}

	public void addToVerwendungszweck(final String string) {
		if (StringUtils.isBlank(verwendungszweck)) {
			verwendungszweck = string == null ? null : string.trim();
		} else {
			verwendungszweck += " ";
			verwendungszweck += string.trim();
		}
	}

	public String getVerwendungszweckClean() {
		return verwendungszweck
				.replaceAll("(SVWZ\\+|EREF\\+\\S* ?+|KREF\\+\\S* ?+|MREF\\+\\S* ?+)", "");
	}

	public String getBuchungstextClean() {
		return buchungstext
				.replace("UE", "Ü");
	}

	public BigDecimal getBetragMitVorzeichen() {
		if (isDebit()) {
			return betrag.negate();
		} else {
			return betrag;
		}
	}

	public void addToName(final String string) {
		if (StringUtils.isBlank(name)) {
			name = string == null ? null : string.trim();
		} else {
			name += " ";
			name += string.trim();
		}
	}

	public String getAsMT940() {
		return ":20:STARTUMS\n"
				+ ":25:" + getKontobezeichnung() + "\n"
				+ ":28C:1\n"
				+ ":60F:C" + DATE.format(getValutaDatum()) + "EUR0,00\n" // we don't know, but it's obligatory => use 0
				+ ":61:" + DATE.format(valutaDatum) + DATE_NOYEAR.format(valutaDatum) + sollHabenKennung.getAbbrev()
				+ "R" + waehrung.format(betrag) + "N" + "NONREF" + "\n"
				+ ":86:" + sollHabenKennung.getGvc() + "?00" + buchungstext + getMt940VerwendungszweckString()
				+ "?30" + bankKennung + "?31" + kontoNummer + "?32" + getMt940StringPart(name, 0)
				+ (isNotEmpty(getMt940StringPart(name, 1)) ? "?33" + getMt940StringPart(name, 1) : "") + "\n"
				+ ":62F:C" + DATE.format(getValutaDatum()) + "EUR0,00"; // we don't know, but it's obligatory => use 0
	}

	String getMt940VerwendungszweckString() {
		String verw = getVerwendungszweckClean();

		int field = 20;
		StringBuilder result = new StringBuilder();

		while (isNotEmpty(getMt940StringPart(verw, field - 20)) && field <= 29) {
			result.append("?").append(field).append(getMt940StringPart(verw, field - 20));
			field++;
		}

		return result.toString();
	}

	/** @param partIndex 0-based */
	private String getMt940StringPart(String str, int partIndex) {
		if (str.length() < partIndex * 27) {
			return "";
		} else if (str.length() < (partIndex + 1) * 27) {
			return str.substring(partIndex * 27);
		} else {
			return str.substring(partIndex * 27, (partIndex + 1) * 27);
		}
	}
}
