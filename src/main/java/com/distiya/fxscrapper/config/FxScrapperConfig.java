package com.distiya.fxscrapper.config;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.predict.FxPredictGrpc;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.service.IAccountService;
import com.distiya.fxscrapper.service.IHistoryService;
import com.distiya.fxscrapper.service.IStreamService;
import com.distiya.fxscrapper.util.AppUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.primitives.Currency;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class FxScrapperConfig {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired(required = false)
    private IStreamService streamService;

    @Bean("oandaContext")
    public Context getOandaContext(){
        Context ctx = new Context(appConfigProperties.getBroker().getApiUrl(),appConfigProperties.getBroker().getApiToken());
        return ctx;
    }

    @Bean("currentAccount")
    public AccountID getCurrentAccount(){
        return new AccountID(appConfigProperties.getBroker().getApiAccount());
    }

    @Bean
    Jackson2JsonDecoder getJacksonToJsonDecoder(ObjectMapper objectMapper){
        return new Jackson2JsonDecoder(objectMapper);
    }

    @Bean("streamWebClient")
    public WebClient getStreamWebClient(Jackson2JsonDecoder jackson2JsonDecoder){
        return WebClient.builder()
                .codecs(c->c.defaultCodecs().serverSentEventDecoder(jackson2JsonDecoder))
                .codecs(c->c.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder))
                .baseUrl(appConfigProperties.getBroker().getApiStreamUrl())
                .defaultHeader("Authorization","Bearer "+appConfigProperties.getBroker().getApiToken())
                .build();
    }

    @Bean("portfolioStatus")
    @DependsOn("oandaContext")
    public PortfolioStatus createPortfolioStatus(AccountID currentAccount,IAccountService accountService,IHistoryService historyService){
        if(streamService != null)
            streamService.getTransactions().subscribe();
        PortfolioStatus portfolioStatus = new PortfolioStatus();
        accountService.getAccount(currentAccount).ifPresent(a->{
            portfolioStatus.setMargin(a.getMarginAvailable().doubleValue());
            portfolioStatus.setLowTradingGranularity(AppUtil.getCandlestickGranularity(appConfigProperties.getBroker().getLowTimeFrame()));
            portfolioStatus.setHighTradingGranularity(AppUtil.getCandlestickGranularity(appConfigProperties.getBroker().getHighTimeFrame()));
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
                            historyService.requestHistory(ti.getInstrument().getName(),portfolioStatus.getLowTradingGranularity(),5000l)
                                    .map(cl->cl.stream().collect(Collectors.toList()))
                                    .ifPresent(cdl->ti.setLowTimeMarketHistory(cdl));
                            historyService.requestHistory(ti.getInstrument().getName(),portfolioStatus.getHighTradingGranularity(),5000l)
                                    .map(cl->cl.stream().collect(Collectors.toList()))
                                    .ifPresent(cdl->ti.setHighTimeMarketHistory(cdl));
                            ti.setPreviousHighMarket(ti.getHighTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue()-2).getMid());
                            ti.setCurrentHighMarket(ti.getHighTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue()-1).getMid());
                            ti.setPreviousLowMarket(ti.getLowTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue()-2).getMid());
                            ti.setCurrentLowMarket(ti.getLowTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue()-1).getMid());
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

    @Bean
    public ManagedChannel getPredictServiceSyncManagedChannel(){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(appConfigProperties.getPredictService().getHost(),appConfigProperties.getPredictService().getPort())
                .usePlaintext()
                .build();
        return channel;
    }

    @Bean
    public FxPredictGrpc.FxPredictBlockingStub getPredictServiceSyncClient(ManagedChannel channel){
        FxPredictGrpc.FxPredictBlockingStub predictClient = FxPredictGrpc.newBlockingStub(channel);
        return predictClient;
    }
}
