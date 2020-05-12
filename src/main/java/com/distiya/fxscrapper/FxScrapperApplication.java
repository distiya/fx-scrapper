package com.distiya.fxscrapper;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(AppConfigProperties.class)
@EnableFeignClients
@EnableScheduling
@Slf4j
public class FxScrapperApplication {

	@Autowired
	private AppConfigProperties appConfigProperties;

	public static void main(String[] args) {
		SpringApplication.run(FxScrapperApplication.class, args);
	}

	@PostConstruct
	public void init(){
		TimeZone.setDefault(TimeZone.getTimeZone(appConfigProperties.getBroker().getTimeZone()));
	}

}
