package org.zephyrsoft.optigemspoonfeeder;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("org.zephyrsoft.optigem-spoonfeeder")
@Getter
@Setter
public class OptigemSpoonfeederProperties {
	private Path dir;
}
