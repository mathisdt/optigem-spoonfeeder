package org.zephyrsoft.optigemspoonfeeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OptigemSpoonfeederApplication {

	public static void main(String[] args) {
		SpringApplication.run(OptigemSpoonfeederApplication.class, args);
	}

}
