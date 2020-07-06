package com.distiya.fxscrapper.domain;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedTickerProperties;
import com.distiya.fxscrapper.service.IPredictService;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.Candlestick;
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
    private CandlestickGranularity lowTradingGranularity;
    private CandlestickGranularity highTradingGranularity;
    private List<Instrument> tradableInstruments;
    private Set<InstrumentName> allPairs = new HashSet<>();
    private Map<String, List<SupportedTickerProperties>> indexTickers;

    @PostConstruct
    public void initializePortfolioWarmingUp(){
        //initializeWarmUpForGranularity(this.getLowTradingGranularity());
        //initializeWarmUpForGranularity(this.getHighTradingGranularity());
        indexTickers = appConfigProperties.getBroker().getSupportedTickers().stream().filter(st -> st.getIsIndex()).collect(Collectors.groupingBy(st -> st.getTicker()));
        this.setIsWarmingUp(false);
        log.info("PortfolioStatus constructed");
    }

    @AllArgsConstructor
    @Getter
    @Setter
    @NoArgsConstructor
    public class CandlestickDataMiniBatch{
        private CandlestickGranularity granularity;
        private InstrumentName instrumentName;
        private List<Candlestick> miniBatch;
    }

    private PortfolioStatus combineAllMinBatches(Object[] batches){
        Arrays.stream(batches)
                .map(e->(CandlestickDataMiniBatch)e)
                .filter(e->e.getMiniBatch().size()==appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue())
                .forEach(mb->{
                    if(mb.getGranularity().equals(this.getLowTradingGranularity()))
                        this.tradeInstrumentMap.get(mb.getInstrumentName().toString()).setLowTimeMarketHistory(mb.getMiniBatch());
                    else if(mb.getGranularity().equals(this.getHighTradingGranularity()))
                        this.tradeInstrumentMap.get(mb.getInstrumentName().toString()).setHighTimeMarketHistory(mb.getMiniBatch());
                });
        return this;
    }

    private void printPredictData(CandlestickGranularity granularity){
        this.tradeInstrumentMap.values().forEach(ti->{
            if(granularity.equals(this.getLowTradingGranularity()) && ti.getLowTimeMarketHistory() != null){
                Candlestick currentMarket = ti.getLowTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue() - 1);
                ti.getCurrentEmaLowIndicator().update(currentMarket.getMid());
                ti.getCurrentStochasticLowIndicator().update(currentMarket.getMid());
                //ti.getCurrentEmaHighIndicator().getSlowEMA(),ti.getCurrentEmaHighIndicator().getFastEMA(),ti.getCurrentStochasticHighIndicator().getKP(),ti.getCurrentStochasticHighIndicator().getDP(),ti.getCurrentStochasticHighIndicator().getDnP()
                log.info("SIGNAL-INDICATE,{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",granularity,ti.getInstrument().getName(),currentMarket.getTime(),currentMarket.getMid().getO(),currentMarket.getMid().getC(),currentMarket.getMid().getH(),currentMarket.getMid().getL(),ti.getCurrentLowPredicted().getO(),ti.getCurrentLowPredicted().getC(),ti.getCurrentLowPredicted().getH(),ti.getCurrentLowPredicted().getL(),ti.getEmaLowIndicator().getSlowEMA(),ti.getEmaLowIndicator().getFastEMA(),ti.getStochasticLowIndicator().getKP(),ti.getStochasticLowIndicator().getDP(),ti.getStochasticLowIndicator().getDnP(),ti.getCurrentEmaLowIndicator().getSlowEMA(),ti.getCurrentEmaLowIndicator().getFastEMA(),ti.getCurrentStochasticLowIndicator().getKP(),ti.getCurrentStochasticLowIndicator().getDP(),ti.getCurrentStochasticLowIndicator().getDnP());
            }
            else if(granularity.equals(this.getHighTradingGranularity()) && ti.getLowTimeMarketHistory() != null){
                Candlestick currentMarket = ti.getHighTimeMarketHistory().get(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue() - 1);
                ti.getCurrentEmaHighIndicator().update(currentMarket.getMid());
                ti.getCurrentStochasticHighIndicator().update(currentMarket.getMid());
                log.info("SIGNAL-INDICATE,{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",granularity,ti.getInstrument().getName(),currentMarket.getTime(),currentMarket.getMid().getO(),currentMarket.getMid().getC(),currentMarket.getMid().getH(),currentMarket.getMid().getL(),ti.getCurrentHighPredicted().getO(),ti.getCurrentHighPredicted().getC(),ti.getCurrentHighPredicted().getH(),ti.getCurrentHighPredicted().getL(),ti.getEmaHighIndicator().getSlowEMA(),ti.getEmaHighIndicator().getFastEMA(),ti.getStochasticHighIndicator().getKP(),ti.getStochasticHighIndicator().getDP(),ti.getStochasticHighIndicator().getDnP(),ti.getCurrentEmaHighIndicator().getSlowEMA(),ti.getCurrentEmaHighIndicator().getFastEMA(),ti.getCurrentStochasticHighIndicator().getKP(),ti.getCurrentStochasticHighIndicator().getDP(),ti.getCurrentStochasticHighIndicator().getDnP());
            }
        });
    }

    private void initializeWarmUpForGranularity(CandlestickGranularity granularity){
        Map<InstrumentName,List<Candlestick>> historyMap = new HashMap<>();
        if(granularity.equals(this.getLowTradingGranularity())){
            tradeInstrumentMap.values().forEach(ti->{
                historyMap.put(ti.getInstrument().getName(),ti.getLowTimeMarketHistory());
            });
        }
        else if(granularity.equals(this.getHighTradingGranularity())){
            tradeInstrumentMap.values().forEach(ti->{
                historyMap.put(ti.getInstrument().getName(),ti.getHighTimeMarketHistory());
            });
        }
        List<Flux<CandlestickDataMiniBatch>> AllMiniBatchFluxes = historyMap.keySet().stream().map(k -> Flux.fromIterable(historyMap.get(k)).buffer(appConfigProperties.getBroker().getDefaultPredictBatchLength().intValue(), 1).map(mb -> new CandlestickDataMiniBatch(granularity,k, mb))).collect(Collectors.toList());
        Flux.zip(AllMiniBatchFluxes,this::combineAllMinBatches)
                .doOnNext(ps->{
                    if(granularity.equals(this.getLowTradingGranularity()))
                        predictService.getPredictionsForPortfolio(this.getLowTradingGranularity(),ps);
                    else if(granularity.equals(this.getHighTradingGranularity()))
                        predictService.getPredictionsForPortfolio(this.getHighTradingGranularity(),ps);
                    printPredictData(granularity);
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
}
