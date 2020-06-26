package com.distiya.fxscrapper.controller;

import com.distiya.fxscrapper.domain.AdminConfig;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @PostMapping(value = "/config",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody  AdminConfig changeConfig(@RequestBody AdminConfig adminConfig){
        if(adminConfig.getTakeProfitPercentage() != null && adminConfig.getTakeProfitPercentage() > 0)
            appConfigProperties.getBroker().setMinTradeProfitPercentage(adminConfig.getTakeProfitPercentage());
        if(adminConfig.getSkipFutureTrades() != null)
            appConfigProperties.getBroker().setSkipFutureTrades(adminConfig.getSkipFutureTrades());
        if(adminConfig.getLeftMarginAmount() != null && adminConfig.getLeftMarginAmount() > 0)
            appConfigProperties.getBroker().setLeftMargin(adminConfig.getLeftMarginAmount());
        return adminConfig;
    }
}