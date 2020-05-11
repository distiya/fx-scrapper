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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.distiya.fxscrapper.util.AppUtil.*;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
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
            //testPredictions();
            placeAllOrders();
            log.info("{}|Ending trading",getCurrentTime());
        }
        catch (Exception e){
            log.error("Error in trading : {}",e.getMessage());
        }
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
        double totalFraction = portfolioStatus.getTradeInstrumentMap().values().stream().mapToDouble(t -> t.getCurrentPredicted().getH().doubleValue()-t.getCurrentPredicted().getL().doubleValue()).sum();
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            double fraction = (ti.getCurrentPredicted().getH().doubleValue()-ti.getCurrentPredicted().getL().doubleValue())/totalFraction;
            ti.setCurrentFraction(fraction);
        });
    }

    private void updateAllMaxUnitCount(){
        portfolioStatus.getTradeInstrumentMap().values().stream().forEach(ti->{
            AppUtil.getUnitCount(ti.getInstrument().getName().toString(),portfolioStatus);
        });
    }

    private void placeAllOrders(){
        Optional<List<Trade>> openTrades = tradeService.getOpenTradesForCurrentAccount();
        Optional<List<Order>> openOrders = orderService.getOpenOrdersForCurrentAccount();
        openOrders.ifPresent(ol->{
            ol.forEach(o->orderService.cancelOrderForCurrentUser(new TransactionID(o.getId().toString())));
        });
        openTrades.ifPresent(ot->ot.stream().forEach(t->{
            tradeService.closeTradeForCurrentAccount(t.getId());
        }));
        portfolioStatus.getTradeInstrumentMap().values().forEach(ti->placeOrderForTradeInstrument(ti));
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

    private void placeOrderForTradeInstrument(TradeInstrument ti){
        if(ti.getCurrentPredicted() != null && ti.getPreviousPredicted() != null){
            double highPrice = calculateHighPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
            double lowPrice = calculateLowPrice(ti.getCurrentMarket(),ti.getCurrentPredicted(),ti.getPreviousPredicted());
            log.info("{}|CURRENT_MARKET|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),ti.getCurrentMarket().getH(),ti.getCurrentMarket().getL());
            log.info("{}|NEW_PREDICT|{}|H:{}|L:{}",getCurrentTime(),ti.getInstrument().getName(),highPrice,lowPrice);
            Optional<BoundedLimitOrder> boundedLimitOrder = orderService.placeBoundLimitOrderForCurrentAccount(ti.getInstrument().getName(), ti.getMaxUnits(), lowPrice, highPrice);
            boundedLimitOrder.ifPresent(blo->ti.setCurrentOrder(blo));
        }
    }
}
