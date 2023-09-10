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
	private Map<String, String> hibiscusServerAccount;
}
