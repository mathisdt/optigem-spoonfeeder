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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Mt940Entry {
	public enum SollHabenKennung {
		/** money was transferred to the current account */
		CREDIT,
		/** money was transferred away from the current account */
		DEBIT
	}

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("YYMMdd");

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

	public String getOriginalTextComplete() {
		return ":20:STARTUMS\n"
				+ ":25:" + getKontobezeichnung() + "\n"
				+ ":28C:1\n"
				+ ":60F:C" + DATE.format(getValutaDatum()) + "EUR0,00\n" // we don't know, so use zero
				+ ":61:\n" // TODO
				+ ":86:\n" // TODO
				+ ":62F:C" + DATE.format(getValutaDatum()) + "EUR0,00"; // we don't know, so use zero
	}
}
