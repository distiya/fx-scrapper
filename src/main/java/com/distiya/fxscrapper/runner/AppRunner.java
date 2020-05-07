package com.distiya.fxscrapper.runner;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.properties.SupportedTickerProperties;
import com.distiya.fxscrapper.service.OandaHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements CommandLineRunner {

    @Autowired
    private OandaHistoryService oandaHistoryService;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public void run(String... args) throws Exception {
        appConfigProperties.getBroker().getSupportedTickers().stream().filter(SupportedTickerProperties::getEnabled)
                .forEach(t->{
                    appConfigProperties.getBroker().getSupportedResolutions().stream().filter(SupportedResolutionProperties::getEnabled)
                            .forEach(r->{
                                oandaHistoryService.requestHistory(t.getTicker(),r, appConfigProperties.getBroker().getStartDate());
                            });
                });
    }
}
