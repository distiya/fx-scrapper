package com.distiya.fxscrapper.controller;

import com.distiya.fxscrapper.domain.AdminConfig;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminController {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @PostMapping(value = "/config",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody  AdminConfig changeConfig(@RequestBody AdminConfig adminConfig){
        log.info("Setting admin config started");
        if(adminConfig.getTakeProfitPercentage() != null && adminConfig.getTakeProfitPercentage() > 0)
            appConfigProperties.getBroker().setMinTradeProfitPercentage(adminConfig.getTakeProfitPercentage());
        if(adminConfig.getSkipFutureTrades() != null)
            appConfigProperties.getBroker().setSkipFutureTrades(adminConfig.getSkipFutureTrades());
        if(adminConfig.getLeftMarginAmount() != null && adminConfig.getLeftMarginAmount() > 0)
            appConfigProperties.getBroker().setLeftMargin(adminConfig.getLeftMarginAmount());
        if(adminConfig.getExtremeProfitPercentage() != null && adminConfig.getExtremeProfitPercentage() > 0)
            appConfigProperties.getBroker().setExtremeTradeProfitPercentage(adminConfig.getExtremeProfitPercentage());
        if(adminConfig.getCutoffProfitPercentage() != null && adminConfig.getCutoffProfitPercentage() > 0)
            appConfigProperties.getBroker().setCutoffTradeProfitPercentage(adminConfig.getCutoffProfitPercentage());
        if(adminConfig.getInvestingFactor() != null && adminConfig.getInvestingFactor() > 0)
            appConfigProperties.getBroker().setInvestingFactor(adminConfig.getInvestingFactor());
        if(adminConfig.getMaxTakeProfitPercentage() != null && adminConfig.getMaxTakeProfitPercentage() > 0)
            appConfigProperties.getBroker().setMaxTradeProfitPercentage(adminConfig.getMaxTakeProfitPercentage());
        log.info("Setting admin config completed");
        return convertToAdminConfig(appConfigProperties);
    }

    @GetMapping(value = "/config",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody  AdminConfig changeConfig(){
        log.info("Getting admin config started");
        return convertToAdminConfig(appConfigProperties);
    }

    private AdminConfig convertToAdminConfig(AppConfigProperties appConfigProperties){
        AdminConfig config = new AdminConfig();
        config.setLeftMarginAmount(appConfigProperties.getBroker().getLeftMargin());
        config.setSkipFutureTrades(appConfigProperties.getBroker().getSkipFutureTrades());
        config.setTakeProfitPercentage(appConfigProperties.getBroker().getMinTradeProfitPercentage());
        config.setExtremeProfitPercentage(appConfigProperties.getBroker().getExtremeTradeProfitPercentage());
        config.setCutoffProfitPercentage(appConfigProperties.getBroker().getCutoffTradeProfitPercentage());
        config.setInvestingFactor(appConfigProperties.getBroker().getInvestingFactor());
        config.setMaxTakeProfitPercentage(appConfigProperties.getBroker().getMaxTradeProfitPercentage());
        return config;
    }
}
