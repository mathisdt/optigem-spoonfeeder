package org.zephyrsoft.optigemspoonfeeder.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BuchungForExport {
	private final Buchung buchung;
	private final OptigemSpoonfeederProperties properties;

	public LocalDate getDatum() {
		return buchung.getDatum();
	}

	public int getSollHK() {
		return buchung.getIncoming() ? properties.getGegenHauptkonto() : buchung.getHauptkonto();
	}

	public int getSollUK() {
		return buchung.getIncoming() ? properties.getGegenUnterkonto() : buchung.getUnterkonto();
	}

	public int getSollProj() {
		return buchung.getIncoming() ? properties.getGegenProjekt() : buchung.getProjekt();
	}

	public int getHabenHK() {
		return !buchung.getIncoming() ? properties.getGegenHauptkonto() : buchung.getHauptkonto();
	}

	public int getHabenUK() {
		return !buchung.getIncoming() ? properties.getGegenUnterkonto() : buchung.getUnterkonto();
	}

	public int getHabenProj() {
		return !buchung.getIncoming() ? properties.getGegenProjekt() : buchung.getProjekt();
	}

	public BigDecimal getBetrag() {
		return buchung.getBetrag();
	}

	public String getBuchText() {
		return buchung.getBuchungstext();
	}
}
