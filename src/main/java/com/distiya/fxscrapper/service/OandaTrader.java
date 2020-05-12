package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.BoundedLimitOrder;
import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.util.AppUtil;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.order.Order;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.transaction.TransactionID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.distiya.fxscrapper.util.AppUtil.*;

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

    private Set<Trade> firstAgeOpenTrades = new HashSet<>();
    private Set<Trade> secondAgeOpenTrades = new HashSet<>();
    private Set<Trade> thirdAgeOpenTrades = new HashSet<>();

    private Set<Order> firstAgeOpenOrders = new HashSet<>();
    private Set<Order> secondAgeOpenOrders = new HashSet<>();
    private Set<Order> thirdAgeOpenOrders = new HashSet<>();

    private Optional<List<Trade>> currentOpenTrades = Optional.empty();
    private Optional<List<Order>> currentOpenOrders = Optional.empty();

    @Scheduled(cron = "${app.config.broker.orderPlacing}")
    public void trade(){
        try{
            log.info("{}|Starting trading",getCurrentTime());
            portfolioStatus.setAccount(accountService.getCurrentAccount().orElse(portfolioStatus.getAccount()));
            portfolioStatus.setMargin(portfolioStatus.getAccount().getMarginAvailable().doubleValue()-appConfigProperties.getBroker().getLeftMargin());
            updateAllMarketCandles(appConfigProperties.getBroker().getDefaultPredictBatchLength());
            predictService.getPredictionsForPortfolio(portfolioStatus);
            updateAllCurrentFraction();
            updateAllMaxUnitCount();
            upgradeOpenTradesAndOrdersGeneration();
            //testPredictions();
            placeAllOrders();
            log.info("{}|Ending trading",getCurrentTime());
        }
        catch (Exception e){
            log.error("Error in trading : {}",e.getMessage());
        }
    }

    private void upgradeOpenTradesAndOrdersGeneration(){

        currentOpenTrades = tradeService.getOpenTradesForCurrentAccount();
        currentOpenOrders = orderService.getOpenOrdersForCurrentAccount();

        currentOpenTrades.ifPresent(ot->{
            ot.stream().forEach(t->{
                if(thirdAgeOpenTrades.contains(t)){
                    tradeService.closeTradeForCurrentAccount(t.getId());
                    thirdAgeOpenTrades.remove(t);
                }
                else if(secondAgeOpenTrades.contains(t)){
                    thirdAgeOpenTrades.add(t);
                    secondAgeOpenTrades.remove(t);
                    log.info("Trade {} got promoted to third generation",t.getId());
                }
                else if(firstAgeOpenTrades.contains(t)){
                    secondAgeOpenTrades.add(t);
                    firstAgeOpenTrades.remove(t);
                    log.info("Trade {} got promoted to second generation",t.getId());
                }
                else
                    firstAgeOpenTrades.add(t);
            });
        });

        currentOpenOrders.ifPresent(ot->{
            ot.stream().forEach(t->{
                if(thirdAgeOpenOrders.contains(t)){
                    orderService.cancelOrderForCurrentUser(new TransactionID(t.getId().toString()));
                    thirdAgeOpenOrders.remove(t);
                }
                else if(secondAgeOpenOrders.contains(t)){
                    thirdAgeOpenOrders.add(t);
                    secondAgeOpenOrders.remove(t);
                    log.info("Order {} got promoted to third generation",t.getId());
                }
                else if(firstAgeOpenOrders.contains(t)){
                    secondAgeOpenOrders.add(t);
                    firstAgeOpenOrders.remove(t);
                    log.info("Order {} got promoted to second generation",t.getId());
                }
                else
                    firstAgeOpenOrders.add(t);
            });
        });
    }

    private void updateAllMarketCandles(long count){
        portfolioStatus.getAllPairs().stream().forEach(t->{
            historyService.requestHistory(t,portfolioStatus.getTradingGranularity(),count)
                    .ifPresent(cl->{
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getTradeInstrumentMap(),t.toString());
                        updateCurrentMarketCandle(count,cl,portfolioStatus.getHomeInstrumentMap(),t.toString());
                    });
        });
    }

    private void updateCurrentMarketCandle(long count,List<Candlestick> cl, Map<String, TradeInstrument> timap, String key){
        if(cl.size() == count && timap.containsKey(key)){
            TradeInstrument tradeInstrument = timap.get(key);
            tradeInstrument.setPreviousMarket(cl.get(cl.size()-2).getMid());
            tradeInstrument.setCurrentMarket(cl.get(cl.size()-1).getMid());
            tradeInstrument.setMarketHistory(cl.stream().map(c->c.getMid()).collect(Collectors.toList()));
        }
    }

    private void updateAllCurrentFraction(){
        double totalFraction = portfolioStatus.getTradeInstrumentMap().values().stream().mapToDouble(t -> formatPrice(t.getCurrentPredicted().getH().doubleValue())-formatPrice(t.getCurrentPredicted().getL().doubleValue())).filter(diff->diff >=appConfigProperties.getBroker().getMinCandleDiff()).sum();
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            double diff = (formatPrice(ti.getCurrentPredicted().getH().doubleValue())-formatPrice(ti.getCurrentPredicted().getL().doubleValue()));
            diff = diff >= appConfigProperties.getBroker().getMinCandleDiff() ? diff : 0.0d;
            double fraction = diff/totalFraction;
            ti.setCurrentFraction(fraction);
        });
    }

    private void updateAllMaxUnitCount(){
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            AppUtil.getUnitCount(ti.getInstrument().getName().toString(),portfolioStatus);
        });
    }

    private void placeAllOrders(){
        portfolioStatus.getTradeInstrumentMap().values().stream().filter(ti->ti.getMaxUnits() > 0).forEach(ti->placeOrderForTradeInstrument(ti));
    }

    private void placeOrderForTradeInstrument(TradeInstrument ti){
        if(ti.getCurrentPredicted() != null && ti.getPreviousPredicted() != null){
            double highPrice = calculateHighPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
            double lowPrice = calculateLowPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
            log.info("{}|CURRENT_MARKET|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),ti.getCurrentMarket().getH(),ti.getCurrentMarket().getL());
            log.info("{}|NEW_PREDICT|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),highPrice,lowPrice);
            Optional<Trade> existingTrade = currentOpenTrades.flatMap(otl -> otl.stream().filter(t -> t.getInstrument().equals(ti.getInstrument().getName())).findFirst());
            if(existingTrade.isEmpty()){
                Optional<BoundedLimitOrder> boundedLimitOrder = orderService.placeBoundLimitOrderForCurrentAccount(ti.getInstrument().getName(), ti.getMaxUnits(), lowPrice, highPrice);
                boundedLimitOrder.ifPresent(blo->ti.setCurrentOrder(blo));
            }
            else
                log.info("There is an existing trade therefore skipping placing a trade");
        }
    }
    private void testPredictions(){
        portfolioStatus.getTradeInstrumentMap().values().forEach(ti->{
            if(ti.getCurrentPredicted() != null && ti.getPreviousPredicted() != null){
                double highPrice = calculateHighPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
                double lowPrice = calculateLowPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
                log.info("{}|CURRENT_MARKET|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),ti.getCurrentMarket().getH(),ti.getCurrentMarket().getL());
                log.info("{}|NEW_PREDICT|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),highPrice,lowPrice);
            }
        });
    }
}
