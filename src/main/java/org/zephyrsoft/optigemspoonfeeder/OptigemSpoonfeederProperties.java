package org.zephyrsoft.optigemspoonfeeder;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

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
	public static class AccountProperties {
		private String name;
		private String description;
		private String tableAccounts;
		private String tableProjects;
	}

	public AccountProperties getBankAccountByDescription(final String description) {
		return bankAccount.values().stream()
			.filter(a -> a.getDescription() != null && a.getDescription().equalsIgnoreCase(description))
			.findAny()
			.orElse(null);
	}
}
