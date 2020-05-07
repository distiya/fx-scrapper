package com.distiya.fxscrapper.config;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.oanda.v20.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FxScrapperConfig {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Bean
    public Context getOandaContext(){
        Context ctx = new Context(appConfigProperties.getBroker().getApiUrl(),appConfigProperties.getBroker().getApiToken());
        return ctx;
    }
}
