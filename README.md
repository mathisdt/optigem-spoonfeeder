![license](https://img.shields.io/github/license/mathisdt/optigem-spoonfeeder.svg?style=flat) [![Build](https://github.com/mathisdt/optigem-spoonfeeder/actions/workflows/build.yaml/badge.svg)](https://github.com/mathisdt/optigem-spoonfeeder/actions) [![last released](https://img.shields.io/github/release-date/mathisdt/optigem-spoonfeeder.svg?label=last%20released&style=flat)](https://github.com/mathisdt/optigem-spoonfeeder/releases)

*Disclaimer: I'm in neither associated with nor paid by Optigem GmbH. I just happen to use their software.*

*This readme is in German as Optigem is a German-only product (it's not internationalized).*

# Optigem Spoonfeeder

## Wofür ist dieses Projekt

Dieses Tool nutzt [Hibiscus Payment Server](https://www.willuhn.de/products/hibiscus-server/) zum Abrufen von
Kontobuchungen einer Bank und erzeugt [Optigem](https://www.optigem.com/produkte/win-finanz/)-Buchungen daraus.
Sie können die Regeln bearbeiten, nach denen die Zuordnung erfolgt (sie sind in Groovy geschrieben, Beispiel unten),
und Sie können das Ergebnis auch im Web-Frontend sehen, bevor Sie es in Optigem importieren. Alle Bankbuchungen, die
nicht von den Regeln erfasst werden, können Sie sie als MT940 exportieren und in Optigem zuordnen (auf herkömmliche Weise).

## Wie man beginnt

Holen Sie sich ein Release-JAR dieses Projekts und Java 17 oder höher. Konfigurieren Sie
die Grundlagen (siehe nächsten Abschnitt), starten Sie die JAR-Datei (`java -jar filename.jar`)
und rufen Sie die Adresse `http://localhost:8080` im Browser auf.

### Konfiguration

Sie können eine Datei namens `application-local.yaml` in das Verzeichnis, von dem aus Sie das JAR
starten ("aktuelles Arbeitsverzeichnis"). In dieser Datei können Sie einige Konfigurationen vornehmen, von
denen nur die erste (das Verzeichnis für Regeln und Tabellen) verpflichtend ist. Beispiel:

```
org:
  zephyrsoft:
    optigem-spoonfeeder:
      dir: /home/yourusername/.optigem-spoonfeeder

      hibiscus-server-url: https://192.168.0.10:8080/xmlrpc/
      hibiscus-server-username: admin
      hibiscus-server-password: yourpassword

      bank-account:
        DE63287492836458292740:
          name: Giro
          table-accounts: girokonten
          table-projects: giroprojekte
        DE89284750281023426171:
          name: Savings

      gegen-hauptkonto: 1200
      gegen-unterkonto: 0
      gegen-projekt: 0
```

Die erste Zeile definiert, wo sich Ihre anderen Konfigurationsdateien befinden (siehe folgende Abschnitte).

Der zweite Block verweist auf Ihren Hibiscus Payment Server. Wenn keiner verfügbar ist, lassen Sie dies einfach weg.

Der dritte Block kann "schöne" Namen für Ihre Konten definieren, wenn sie von Hibiscus abgerufen werden.
Wenn Sie kein Hibiscus verwenden, lassen Sie dies auch weg.

Der vierte Block gibt an, welches Optigem-Konto als Bankkonto angenommen werden soll. Die Regeln im nächsten
Abschnitt führen zu einem Optigem-Konto für jede Bank-Buchung, aber Optigem nutzt doppelte Buchführung und
benötigt daher ein Gegenkonto.

### Regeln

Zur Abbildung der Eingangsdaten (Bankbuchungen, die Sie entweder aus einer MT940-Datei importieren oder aus
dem Hibiscus Server lesen können) auf Optigem-Buchungen müssen Sie Regeln definieren. Sie befinden sich in
der Datei `rules.groovy` im oben konfigurierten Verzeichnis. Hier ist ein einfaches Beispiel:

```
if (soll && verwendungszweck.contains("Gehalt")) {
	return buchung(4110)
}

if (soll && name.contains("Knappschaft-Bahn-See", "Krankenkasse")) {
	return buchung(4130)
}
```

Diese Datentypen und zugehörigen Felder gibt es:

* **TEXT**: eigenkonto, buchungstext, verwendungszweck, bank, konto, name
* **ZAHL**: betrag
* **DATUM**: datum
* **JA/NEIN**: soll, haben

Text-Felder besitzen eine `contains`-Methode, die Groß- und Kleinschreibung ignoriert sowie Umlaute
auflöst. Es können beliebig viele Ausdrücke übergeben werden, die ODER-verknüpft gesucht werden
(d.h. sobald auch nur einer der Begriffe gefunden wurde, ist der Wert des Aufrufs TRUE).

Innerhalb der Regelauswertung gibt es die Möglichkeit, Logmeldungen mit eingesetzten Werten auszugeben.
Beispiel:

```
log("Betrag {} am {}", betrag, datum)
```

Optigem-Buchungen können mit diesem Aufruf erzeugt werden:

```
buchung(hauptkonto, unterkonto, projekt, buchungstext)
```

Dabei können von hinten Felder weggelassen werden, z.B. `buchung(8010, 15)`.
Weiter vorn nicht gefüllte Felder können auf `null` gesetzt werden, z.B. `buchung(8010, null, 70)`

### Tabellen

Des weiteren können beliebig viele Tabellen definiert werden. Dies sind simple Dateien (XLS, XLSX oder CSV),
die im konfigurierten Verzeichnis mit dem Namenspräfix `table_` abgelegt werden, z.B. `table_personen.xls`.
In der ersten Zeile **müssen** die Spaltennamen stehen.

Diese Tabellen kann man in der Regelauswertung einbeziehen. Hier ein etwas komplizierteres Beispiel,
in dem die Tabelle "person" mindestens die Spalten "Nr", "Vorname", "Nachname" und "IBAN" enthält:

```
if (haben && verwendungszweck.contains("spende", "opfer", "kollekte")
        && personen.contains("iban", konto)) {
    person = personen.where("iban", konto)
    projekt = null;
    if (verwendungszweck.contains("stichwort1")) {
        projekt = 10
    } else if (verwendungszweck.contains("stichwort2")) {
        projekt = 11
    }
    return buchung(8010, person.get("nr"), projekt,
            "Spende " + person.get("vorname") + " " + person.get("nachname"))
}
```

### Hinweis

Wenn sich an den Regeln oder Tabellen etwas geändert hat, muss die Software nicht neu gestartet werden.
Bei jeder Regelausführung werden diese Dateien neu eingelesen.
