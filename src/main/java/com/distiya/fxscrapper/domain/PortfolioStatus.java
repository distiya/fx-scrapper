package com.distiya.fxscrapper.domain;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.service.IPredictService;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Currency;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.primitives.InstrumentName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class PortfolioStatus {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private IPredictService predictService;

    private double margin = 0;
    private Boolean isWarmingUp = true;
    private Map<String,TradeInstrument> tradeInstrumentMap;
    private Map<String,TradeInstrument> homeInstrumentMap;
    private AccountID currentAccount;
    private Currency homeCurrency;
    private Account account;
    private CandlestickGranularity tradingGranularity;
    private List<Instrument> tradableInstruments;
    private Set<InstrumentName> allPairs = new HashSet<>();

    @PostConstruct
    public void initializePortfolioWarmingUp(){
        Map<InstrumentName,List<CandlestickData>> historyMap = new HashMap<>();
        tradeInstrumentMap.values().forEach(ti->{
            historyMap.put(ti.getInstrument().getName(),ti.getMarketHistory());
        });
        List<Flux<CandlestickDataMiniBatch>> AllMiniBatchFluxes = historyMap.keySet().stream().map(k -> Flux.fromIterable(historyMap.get(k)).buffer(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue(), 1).map(mb -> new CandlestickDataMiniBatch(k, mb))).collect(Collectors.toList());
        Flux.zip(AllMiniBatchFluxes,this::combineAllMinBatches)
                .doOnNext(ps->{
                    predictService.getPredictionsForPortfolio(ps);
                    printPredictData();
                })
                .doOnComplete(()->{
                    this.setIsWarmingUp(false);
                    log.info("All trading instrument warming up completed");
                })
                .doOnSubscribe(s->{
                    log.info("All trading instrument warming up started");
                })
                .subscribe();

    }

    @AllArgsConstructor
    @Getter
    @Setter
    @NoArgsConstructor
    public class CandlestickDataMiniBatch{
        private InstrumentName instrumentName;
        private List<CandlestickData> miniBatch;
    }

    private PortfolioStatus combineAllMinBatches(Object[] batches){
        Arrays.stream(batches)
                .map(e->(CandlestickDataMiniBatch)e)
                .filter(e->e.getMiniBatch().size()==appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue())
                .forEach(mb->this.tradeInstrumentMap.get(mb.getInstrumentName().toString()).setMarketHistory(mb.getMiniBatch()));
        return this;
    }

    private void printPredictData(){
        this.tradeInstrumentMap.values().forEach(ti->{
            if(ti.getCurrentMarket() != null){
                //SIGNAL-INDICATE|Ticker|Close|Slow_EMA|Fast_EMA|KP|DP|DNP|eMA_Signal
                log.info("SIGNAL-INDICATE|{}|{}|{}|{}|{}|{}|{}|{}",ti.getInstrument().getName(),ti.getMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue()-1).getC(),ti.getEmaIndicator().getSlowEMA(),ti.getEmaIndicator().getFastEMA(),ti.getStochasticIndicator().getKP(),ti.getStochasticIndicator().getDP(),ti.getStochasticIndicator().getDnP(),ti.getEmaIndicator().getCurrentSignal());
            }
        });
    }
}
