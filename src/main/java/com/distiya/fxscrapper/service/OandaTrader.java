package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.strategy.ITradeStrategy;
import com.distiya.fxscrapper.util.AppUtil;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.trade.Trade;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.distiya.fxscrapper.util.AppUtil.getCurrentTime;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
@ConditionalOnProperty(prefix = "app.config.broker",name = "tradingMode",havingValue = "true")
public class OandaTrader implements ITrader{

    @Autowired
    private Context context;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IHistoryService historyService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IPricingService pricingService;

    @Autowired
    private IPredictService predictService;

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Autowired
    private PortfolioStatus portfolioStatus;

    @Autowired
    private ITradeService tradeService;

    @Autowired
    private ITradeStrategy strategy;

    private Boolean isDailyPreparationCompleted = false;

    private Boolean skipFirstTrade = false;

    @PostConstruct
    public void initialize(){
        prepareForDailyTrade();
    }

    @Scheduled(cron = "${app.config.broker.dailyPreparation}")
    public void prepareForDailyTrade(){
        if(!this.portfolioStatus.getIsWarmingUp()){
            try{
                log.info("{}|Starting daily preparation",getCurrentTime());
                isDailyPreparationCompleted = false;
                updateAllDailyVolumes();
                //updateAllCurrentFraction();
                isDailyPreparationCompleted = true;
                log.info("{}|Ending daily preparation",getCurrentTime());
            }
            catch (Exception e){
                log.error("Error in daily preparation : {}",e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.config.broker.orderPlacing}")
    public void trade(){
        if(!this.portfolioStatus.getIsWarmingUp() && isDailyPreparationCompleted){
            try{
                log.info("{}|Starting trading",getCurrentTime());
                accountService.getCurrentAccount().ifPresent(a->portfolioStatus.setAccount(a));
                portfolioStatus.setMargin(portfolioStatus.getAccount().getMarginAvailable().doubleValue()-appConfigProperties.getBroker().getLeftMargin());
                updateAllMarketCandles(appConfigProperties.getBroker().getDefaultPredictBatchLength());
                //updateAllMaxUnitCount();
                //predictService.getPredictionsForPortfolio(portfolioStatus.getLowTradingGranularity(),portfolioStatus);
                //predictService.getPredictionsForPortfolio(portfolioStatus.getHighTradingGranularity(),portfolioStatus);
                //placeAllOrders();
                strategy.trade();
                log.info("{}|Ending trading",getCurrentTime());
            }
            catch (Exception e){
                log.error("Error in trading : {}",e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.config.broker.highScreenScheduling}")
    public void scheduleForHighScreen(){
        if(!this.portfolioStatus.getIsWarmingUp()){
            try{
                log.info("{}|Starting setting high screen candle history",getCurrentTime());
                updateAllMarketCandlesForHighScreen(appConfigProperties.getBroker().getDefaultPredictBatchLength());
                log.info("{}|Ending setting high screen candle history",getCurrentTime());
            }
            catch (Exception e){
                log.error("Error in trading : {}",e.getMessage());
            }
        }
    }



    private void updateAllMarketCandles(long count){
        portfolioStatus.getAllPairs().stream().forEach(t->{
            historyService.requestHistory(t,portfolioStatus.getLowTradingGranularity(),count)
                    .ifPresent(cl->{
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getTradeInstrumentMap(),t.toString(),portfolioStatus.getLowTradingGranularity());
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getHomeInstrumentMap(),t.toString(),portfolioStatus.getLowTradingGranularity());
                    });
        });
    }

    private void updateAllMarketCandlesForHighScreen(long count){
        portfolioStatus.getAllPairs().stream().forEach(t->{
            historyService.requestHistory(t,portfolioStatus.getHighTradingGranularity(),count)
                    .ifPresent(cl->{
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getTradeInstrumentMap(),t.toString(),portfolioStatus.getHighTradingGranularity());
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getHomeInstrumentMap(),t.toString(),portfolioStatus.getHighTradingGranularity());
                    });
        });
    }

    private void updateCurrentMarketCandle(long count, List<Candlestick> cl, Map<String, TradeInstrument> timap, String key, CandlestickGranularity granularity){
        if(cl.size() == count && timap.containsKey(key)){
            if(granularity.equals(portfolioStatus.getLowTradingGranularity())){
                TradeInstrument tradeInstrument = timap.get(key);
                tradeInstrument.setPreviousLowMarket(cl.get(cl.size()-2).getMid());
                tradeInstrument.setCurrentLowMarket(cl.get(cl.size()-1).getMid());
                tradeInstrument.setLowTimeMarketHistory(cl);
                tradeInstrument.getCurrentEmaLowIndicator().update(tradeInstrument.getCurrentLowMarket());
                tradeInstrument.getCurrentStochasticLowIndicator().update(tradeInstrument.getCurrentLowMarket());
            }
            else if(granularity.equals(portfolioStatus.getHighTradingGranularity())){
                TradeInstrument tradeInstrument = timap.get(key);
                tradeInstrument.setPreviousHighMarket(cl.get(cl.size()-2).getMid());
                tradeInstrument.setCurrentHighMarket(cl.get(cl.size()-1).getMid());
                tradeInstrument.setHighTimeMarketHistory(cl);
                tradeInstrument.getCurrentEmaHighIndicator().update(tradeInstrument.getCurrentHighMarket());
                tradeInstrument.getCurrentStochasticHighIndicator().update(tradeInstrument.getCurrentHighMarket());
            }
        }
    }

    private void updateAllDailyVolumes(){
        portfolioStatus.getTradeInstrumentMap().values().stream().map(ti->ti.getInstrument().getName()).forEach(t->{
            historyService.requestHistory(t,CandlestickGranularity.D,2l)
                    .ifPresent(cl->{
                        updateCurrentDailyVolume(portfolioStatus.getTradeInstrumentMap(),t.toString(),cl.get(1).getVolume() >= 1 ? cl.get(1).getVolume() : cl.get(0).getVolume());
                    });
        });
    }

    private void updateCurrentDailyVolume(Map<String, TradeInstrument> timap, String key, Long volume){
        timap.get(key).setDailyVolume(volume.doubleValue());
        log.info("Daily volume for instrument {} is {}",key,volume);
    }

    private void updateAllCurrentFraction(){
        double totalFraction = portfolioStatus.getTradeInstrumentMap().values().stream().mapToDouble(ti->ti.getDailyVolume()).sum();
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            ti.setCurrentFraction(totalFraction > 0 ? ti.getDailyVolume()/totalFraction : 0.0);
            log.info("Current fraction for instrument {} is {}",ti.getInstrument().getName(),ti.getCurrentFraction());
        });
    }

    private void updateAllMaxUnitCount(){
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            AppUtil.getUnitCount(ti.getInstrument().getName().toString(),portfolioStatus);
            log.info("Max units for instrument {} is {}",ti.getInstrument().getName(),ti.getMaxUnits());
        });
    }

    private void placeAllOrders(){
        Optional<List<Trade>> currentOpenTrades = tradeService.getOpenTradesForCurrentAccount();
        portfolioStatus.getTradeInstrumentMap().values().stream().filter(ti->ti.getMaxUnits() >= 1).forEach(ti->placeOrderForTradeInstrument(ti,currentOpenTrades));
    }

    private void placeOrderForTradeInstrument(TradeInstrument ti,Optional<List<Trade>> currentOpenTrades){
        Optional<Trade> currentOpenTradeForInstrument = currentOpenTrades.flatMap(otl -> otl.stream().filter(tr -> tr.getInstrument().equals(ti.getInstrument().getName())).findFirst());
        currentOpenTradeForInstrument.ifPresentOrElse(tr->{
            log.info("Trade Exists - {}|{}|{}|{}|{}|{}|{}",tr.getCurrentUnits().doubleValue(),ti.getStochasticLowIndicator().getKP(),ti.getStochasticLowIndicator().getDP(),ti.getStochasticLowIndicator().getDnP(),ti.getStochasticHighIndicator().getKP(),ti.getStochasticHighIndicator().getDP(),ti.getStochasticHighIndicator().getDnP());
            if((tr.getCurrentUnits().doubleValue() > 0) && (ti.getStochasticLowIndicator().getKP() > ti.getStochasticHighIndicator().getKP()) && (ti.getStochasticLowIndicator().getDP() < ti.getStochasticHighIndicator().getKP())){
                tradeService.closeTradeForCurrentAccount(tr.getId());
                ti.setTradeDirection(-1);
                log.info("Trade {} of {} closed and set trade direction sell only because KPL > KPH && DPL < KPH",tr.getId(),ti.getInstrument().getName());
            }
            else if((tr.getCurrentUnits().doubleValue() < 0) && (ti.getStochasticLowIndicator().getKP() < ti.getStochasticHighIndicator().getKP()) && (ti.getStochasticLowIndicator().getDP() > ti.getStochasticHighIndicator().getKP())){
                tradeService.closeTradeForCurrentAccount(tr.getId());
                ti.setTradeDirection(1);
                log.info("Trade {} of {} closed and set trade direction buy only because KPL < KPH && DPL > KPH",tr.getId(),ti.getInstrument().getName());
            }
        },()->{
            log.info("Entering Trade - {}|{}|{}|{}|{}|{}",ti.getStochasticLowIndicator().getKP(),ti.getStochasticLowIndicator().getDP(),ti.getStochasticLowIndicator().getDnP(),ti.getStochasticHighIndicator().getKP(),ti.getStochasticHighIndicator().getDP(),ti.getStochasticHighIndicator().getDnP());
            if((ti.getTradeDirection().equals(1) || ti.getTradeDirection().equals(0)) && (ti.getStochasticLowIndicator().getKP() > ti.getStochasticHighIndicator().getKP()) && (ti.getStochasticHighIndicator().getKP() > ti.getStochasticHighIndicator().getDnP())){
                if(skipFirstTrade){
                    OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * 1.0);
                    log.info("Buy market order placed for {} because KPL > KPH && KPH > DnPH",ti.getInstrument().getName());
                }
                else{
                    ti.setTradeDirection(-1);
                    skipFirstTrade = true;
                    log.info("Skipped first buy signal and set next signal type as sell for instrument {}",ti.getInstrument().getName());
                }
            }
            else if((ti.getTradeDirection().equals(-1) || ti.getTradeDirection().equals(0)) && (ti.getStochasticLowIndicator().getKP() < ti.getStochasticHighIndicator().getKP()) && (ti.getStochasticLowIndicator().getKP() < ti.getStochasticLowIndicator().getDnP())){
                if(skipFirstTrade){
                    OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * -1.0);
                    log.info("Sell market order placed for {} because KPL < KPH && KPL < DnPL",ti.getInstrument().getName());
                }
                else{
                    ti.setTradeDirection(1);
                    skipFirstTrade = true;
                    log.info("Skipped first sell signal and set next signal type as buy for instrument {}",ti.getInstrument().getName());
                }
            }
        });
    }
}
