package com.clearpath.clearpath_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClearpathApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClearpathApiApplication.class, args);
	}

}
