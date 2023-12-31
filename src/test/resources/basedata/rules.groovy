/*
 * Felder der ankommenden Buchung vom Konto:
 *     TEXT: eigenkonto, buchungstext, verwendungszweck, bank, konto, name
 *     ZAHL: betrag
 *     DATUM: datum
 *     JA/NEIN: soll, haben
 *
 * neue Optigem-Buchungen erzeugen:
 *     buchung(hauptkonto, unterkonto, projekt, buchungstext)
 *         - von hinten können Felder weggelassen werden, z.B. buchung(hauptkonto, unterkonto)
 *         - weiter vorn nicht gefüllte Felder können auf null gesetzt werden, z.B. buchung(hauptkonto, null, projekt)
 */
if (verwendungszweck.contains("telefon", "telekom")) {
	return buchung(4940)
}

if (verwendungszweck.contains("troppa", "tropper", "marius")
		&& personen.contains("iban", konto)) {
		person = personen.where("iban", konto)
	return buchung(8010, person.get("nr"), 1, person.get("vorname") + " " + person.get("nachname"))
}

if (verwendungszweck.contains("spende", "opfer", "gemeindebeitrag", "zehnt", "kollekte", "zuwendung")
		&& personen.contains("iban", konto)) {
	person = personen.where("iban", konto)
	return buchung(8010, person.get("nr"), null, person.get("vorname") + " " + person.get("nachname"))
}

if (verwendungszweck.contains("bareinzahlung")) {
	return buchung(1360)
}

if (verwendungszweck.contains("freizeit")
		&& verwendungszweck.contains("gfyc", "teen", "jugend")) {
	return buchung(8205, 4, 21)
}

if (verwendungszweck.contains("freizeit")
		&& (verwendungszweck.contains("root")
		|| (verwendungszweck.contains("jung")
		&& verwendungszweck.contains("erwachsen")))) {
	return buchung(8205, 5, 20)
}

