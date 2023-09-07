// comment
if (verwendungszweck.contains("telefon", "telekom")) {
	buchung = new Buchung(4940)
	done = true
}

if (verwendungszweck.contains("troppa", "tropper", "marius")
		&& personen.contains("iban", konto)) {
	buchung = new Buchung(8010, personen.where("iban", konto).get("nr"), 1)
			done = true
}

if (verwendungszweck.contains("spende", "opfer", "gemeindebeitrag", "zehnt", "kollekte", "zuwendung")
		&& personen.contains("iban", konto)) {
	person = personen.where("iban", konto)
	buchung = new Buchung(8010, person.get("nr"), null, "Spende " + person.get("vorname") + " " + person.get("nachname"))
	done = true
}

if (verwendungszweck.contains("bareinzahlung")) {
	buchung = new Buchung(1360)
	done = true
}

if (verwendungszweck.contains("freizeit")
		&& verwendungszweck.contains("gfyc", "teen", "jugend")) {
	buchung = new Buchung(8205, 4, 21)
	done = true
}

if (verwendungszweck.contains("freizeit")
		&& (verwendungszweck.contains("root")
		|| (verwendungszweck.contains("jung")
		&& verwendungszweck.contains("erwachsen")))) {
	buchung = new Buchung(8205, 5, 20)
	done = true
}

