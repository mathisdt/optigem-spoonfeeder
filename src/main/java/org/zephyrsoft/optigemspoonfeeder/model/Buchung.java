package org.zephyrsoft.optigemspoonfeeder.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE) // for GSON
public class Buchung {
	private LocalDate datum;
	private int hauptkonto;
	private int unterkonto;
	private int projekt;
	/**
	 * if {@code true}, the debit side will be HK=1200, UK=0, PROJ=0 - if
	 * {@code false}, the credit side
	 */
	private Boolean incoming;
	private BigDecimal betrag;
	private String buchungstext;

	public Buchung(Object hauptkonto, Object unterkonto, Object projekt, Object buchungstext) {
		this.hauptkonto = num(hauptkonto);
		this.unterkonto = num(unterkonto);
		this.projekt = num(projekt);
		this.buchungstext = str(buchungstext);
	}

	private String str(Object obj) {
		if (obj == null) {
			return null;
		}
		return obj instanceof String s ? s : obj.toString();
	}

	private Integer num(Object obj) {
		if (obj == null) {
			return 0;
		}
		return obj instanceof Integer i ? i : Integer.valueOf(str(obj));
	}

	public boolean isEmpty() {
		return hauptkonto == 0 || betrag == null || incoming == null || datum == null;
	}
}
