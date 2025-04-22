package org.zephyrsoft.optigemspoonfeeder.service;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.zephyrsoft.optigemspoonfeeder.OptigemSpoonfeederProperties;
import org.zephyrsoft.optigemspoonfeeder.model.Table;
import org.zephyrsoft.optigemspoonfeeder.model.TableRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersistenceService persistenceService;

    public int createPersonAndUnterkonto(OptigemSpoonfeederProperties.AccountProperties accountProperties,
        Table tableOptigemAccounts, int nummer, String vorname, String nachname, String iban) {
        if (accountProperties == null) {
            throw new IllegalArgumentException("kein Bankkonto angegeben");
        }
        if (tableOptigemAccounts == null) {
            throw new IllegalArgumentException("keine Konto-Tabelle konfiguriert");
        }

        final String tablePersons = accountProperties.getTablePersons();
        if (tablePersons == null) {
            throw new IllegalStateException("keine Personen-Tabelle konfiguriert");
        }
        String personsColumnNr = accountProperties.getPersonsColumnNr();
        String personsColumnIban = accountProperties.getPersonsColumnIban();
        String personsColumnVorname = accountProperties.getPersonsColumnVorname();
        String personsColumnNachname = accountProperties.getPersonsColumnNachname();
        String accountsHkForPersons = accountProperties.getAccountsHkForPersons();
        String accountsColumnHk = accountProperties.getAccountsColumnHk();
        String accountsColumnUk = accountProperties.getAccountsColumnUk();
        String accountsColumnBez = accountProperties.getAccountsColumnBez();

        Table persons = persistenceService.getTable(tablePersons);
        if (persons.contains(personsColumnIban, iban)) {
            TableRow person = persons.where(personsColumnIban, iban);
            throw new IllegalStateException("Person mit dieser IBAN bereits vorhanden! " + person.get(personsColumnNr) + " / "
                + person.get(personsColumnVorname) + " " + person.get(personsColumnNachname));
        }
        boolean personWithNumberExists = persons.contains(personsColumnNr, String.valueOf(nummer));
        if (personWithNumberExists) {
            TableRow person = persons.where(personsColumnNr, String.valueOf(nummer));
            throw new IllegalStateException("Nummer " + nummer + " kann nicht verwendet werden, eine Person mit dieser Nummer existiert bereits ("
                + person.get(personsColumnVorname) + " " + person.get(personsColumnNachname) + ")");
        }

        TableRow existingPersonAccount = tableOptigemAccounts.getRows().stream()
            .filter(r -> Objects.equals(r.get(accountsColumnHk), accountsHkForPersons)
                && Objects.equals(r.get(accountsColumnUk), String.valueOf(nummer)))
            .findAny()
            .orElse(null);
        if (existingPersonAccount != null) {
            throw new IllegalStateException("Nummer " + nummer + " kann nicht verwendet werden, eine Unterkonto mit dieser Nummer existiert bereits ("
                + existingPersonAccount.get(personsColumnVorname) + " " + existingPersonAccount.get(personsColumnNachname) + ")");
        }

        TableRow newPersonAccount = new TableRow()
            .with(accountsColumnHk, accountsHkForPersons)
            .with(accountsColumnUk, String.valueOf(nummer))
            .with(accountsColumnBez, vorname + (StringUtils.isNoneBlank(vorname, nachname) ? " " : "") + nachname);
        tableOptigemAccounts.add(newPersonAccount);
        tableOptigemAccounts.sortBy(accountsColumnHk, accountsColumnUk);

        persons.add(new TableRow()
            .with(personsColumnNr, String.valueOf(nummer))
            .with(personsColumnIban, iban)
            .with(personsColumnVorname, vorname)
            .with(personsColumnNachname, nachname));
        persons.sortBy(personsColumnNr);

        persistenceService.write(tableOptigemAccounts);
        persistenceService.write(persons);

        return nummer;
    }
}
