package com.distiya.fxscrapper.config;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.service.IAccountService;
import com.distiya.fxscrapper.util.AppUtil;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.primitives.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class FxScrapperConfig {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private IAccountService accountService;

    @Bean
    public Context getOandaContext(){
        Context ctx = new Context(appConfigProperties.getBroker().getApiUrl(),appConfigProperties.getBroker().getApiToken());
        return ctx;
    }

    @Bean("currentAccount")
    public AccountID getCurrentAccount(){
        return new AccountID(appConfigProperties.getBroker().getApiAccount());
    }

    @Bean("portfolioStatus")
    public PortfolioStatus createPortfolioStatus(AccountID currentAccount){
        PortfolioStatus portfolioStatus = new PortfolioStatus();
        accountService.getAccount(currentAccount).ifPresent(a->{
            portfolioStatus.setMargin(a.getMarginAvailable().doubleValue());
            portfolioStatus.setTradingGranularity(AppUtil.getCandlestickGranularity(appConfigProperties.getBroker().getDefaultResolution()));
            portfolioStatus.setCurrentAccount(currentAccount);
            portfolioStatus.setAccount(a);
            portfolioStatus.setHomeCurrency(new Currency(appConfigProperties.getBroker().getHomeCurrency()));
        });
        accountService.getTradableInstrumentsForCurrentAccount().ifPresent(til->{
            portfolioStatus.setTradableInstruments(til);
            Set<String> tradableInstrumentFilter = appConfigProperties.getBroker().getSupportedTickers().stream().filter(i -> i.getEnabled()).map(i -> i.getTicker()).collect(Collectors.toSet());
            if(!tradableInstrumentFilter.isEmpty()){
                portfolioStatus.setTradeInstrumentMap(new HashMap<>());
                portfolioStatus.setHomeInstrumentMap(new HashMap<>());
                til.stream().filter(i->tradableInstrumentFilter.contains(i.getName().toString()))
                        .map(AppUtil::mapToTradeInstrument)
                        .forEach(ti->{
                            ti.setCurrentFraction(1/tradableInstrumentFilter.size());
                            portfolioStatus.getTradeInstrumentMap().put(ti.getTicker(),ti);
                        });
                Set<String> homeBasePair = tradableInstrumentFilter.stream().flatMap(tp -> Stream.of(AppUtil.getBaseHomePair(tp, appConfigProperties.getBroker().getHomeCurrency())))
                        .collect(Collectors.toSet());
                til.stream().filter(i->homeBasePair.contains(i.getName().toString()))
                        .map(AppUtil::mapToTradeInstrument)
                        .forEach(ti->{
                            ti.setCurrentFraction(1/tradableInstrumentFilter.size());
                            portfolioStatus.getHomeInstrumentMap().put(ti.getTicker(),ti);
                        });
                portfolioStatus.getTradeInstrumentMap().values().forEach(t->portfolioStatus.getAllPairs().add(t.getInstrument().getName()));
                portfolioStatus.getHomeInstrumentMap().values().forEach(t->portfolioStatus.getAllPairs().add(t.getInstrument().getName()));
            }
        });
        return portfolioStatus;
    }
}
