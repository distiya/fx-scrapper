package com.distiya.fxscrapper.runner;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.properties.SupportedTickerProperties;
import com.distiya.fxscrapper.service.IHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.config.broker",name = "scrappingMode",havingValue = "true")
@Slf4j
public class DataLoadRunner implements CommandLineRunner {

    @Autowired
    private IHistoryService oandaHistoryService;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public void run(String... args) throws Exception {
        log.info("Data loading started");
        appConfigProperties.getBroker().getSupportedTickers().stream().filter(SupportedTickerProperties::getEnabled)
                .forEach(t->{
                    appConfigProperties.getBroker().getSupportedResolutions().stream().filter(SupportedResolutionProperties::getEnabled)
                            .forEach(r->{
                                oandaHistoryService.requestHistory(t,r, appConfigProperties.getBroker().getStartDate());
                            });
                });
        log.info("Data loading completed");
    }
}
