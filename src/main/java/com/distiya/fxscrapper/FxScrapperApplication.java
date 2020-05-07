package com.distiya.fxscrapper;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppConfigProperties.class)
public class FxScrapperApplication {

	public static void main(String[] args) {
		SpringApplication.run(FxScrapperApplication.class, args);
	}

}
