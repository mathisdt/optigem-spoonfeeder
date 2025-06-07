package org.zephyrsoft.optigemspoonfeeder;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ConfigurationProperties("org.zephyrsoft.optigem-spoonfeeder")
@Getter
@Setter
public class OptigemSpoonfeederProperties {
	private Path dir;

	private Integer gegenHauptkonto;
	private Integer gegenUnterkonto;
	private Integer gegenProjekt;

	private URL hibiscusServerUrl;
	private String hibiscusServerUsername;
	private String hibiscusServerPassword;
	/** IBAN to description (optional) */
	private Map<String, AccountProperties> bankAccount;

	@Getter
	@Setter
	@ToString
	public static class AccountProperties {
		private String name;
		private String description;
		private String tableAccounts;
		private String tableProjects;
		private String tablePersons;
		private String accountsColumnHk = "Hauptkonto";
		private String accountsColumnUk = "Unterkonto";
		private String accountsColumnBez = "Kontobezeichnung";
		private String projectsColumnNr = "Nr";
		private String projectsColumnBez = "Name";
		private String personsColumnNr = "Nr";
		private String personsColumnIban = "IBAN";
		private String personsColumnVorname = "Vorname";
		private String personsColumnNachname = "Nachname";
		private String accountsHkForPersons = "8010";
		private String paypalBaseUrl = "https://api-m.paypal.com";
		private String paypalAuthEndpoint = "/v1/oauth2/token";
		private String paypalClientId;
		private String paypalClientSecret;
		private int paypalDaysBefore = 6;
	}

	public AccountProperties getBankAccountByDescription(final String description) {
		return bankAccount.values().stream()
			.filter(a -> a.getDescription() != null && a.getDescription().equalsIgnoreCase(description))
			.findAny()
			.orElse(null);
	}
}
