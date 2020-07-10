package com.distiya.fxscrapper.strategy;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.service.IOrderService;
import com.distiya.fxscrapper.service.ITradeService;
import com.distiya.fxscrapper.util.AppUtil;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TripleScreenStrategy implements ITradeStrategy{

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Autowired
    private PortfolioStatus portfolioStatus;

    @Autowired
    private ITradeService tradeService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Override
    public void trade() {
        updateLastTradeCloseSignalForNonOpenedTradeInstruments();
        closeEligibleTrades();
        openEligibleTrades();
    }

    @Override
    public void closeMaxProfitTrades(){
        Optional<List<Trade>> currentOpenTrades = tradeService.getOpenTradesForCurrentAccount();
        currentOpenTrades.ifPresent(otl->{
            otl.stream()
                    .forEach(tr->{
                        double tradeCurrentProfitPercentage = (tr.getUnrealizedPL().doubleValue() / tr.getInitialMarginRequired().doubleValue()) * 100.0;
                        TradeInstrument ti = portfolioStatus.getTradeInstrumentMap().get(tr.getInstrument());
                        if(tradeCurrentProfitPercentage >= appConfigProperties.getBroker().getExtremeTradeProfitPercentage()){
                            ti.setHasExtremeEnd(true);
                        }
                        if(isExtremeEndReversal(ti,tradeCurrentProfitPercentage) || tradeCurrentProfitPercentage >= appConfigProperties.getBroker().getMaxTradeProfitPercentage()){
                            tradeService.closeTradeForCurrentAccount(tr.getId());
                            updateLastTradeCloseSignal(ti);
                            ti.setHasExtremeEnd(false);
                            log.info("Trade {} closed because max profit limit has been reached for {}",tr.getId(),ti.getInstrument().getName());
                        }
                    });
        });
    }

    private void updateLastTradeCloseSignalForNonOpenedTradeInstruments(){
        portfolioStatus.getTradeInstrumentMap().values().stream()
                .filter(ti->ti.getOpenedTradeCount() == 0)
                .forEach(ti-> updateLastTradeCloseSignal(ti));
    }

    private void updateLastTradeCloseSignal(TradeInstrument ti){
        if(getUpperScreenTrend(ti) > 0 && tradeClosableStatus(ti) < 0){
            ti.setLastTradeCloseSignal(1);
        }
        else if(getUpperScreenTrend(ti) < 0 && tradeClosableStatus(ti) > 0){
            ti.setLastTradeCloseSignal(-1);
        }
    }

    private void closeEligibleTrades(){
        Optional<List<Trade>> currentOpenTrades = tradeService.getOpenTradesForCurrentAccount();
        currentOpenTrades.ifPresent(otl->{
            otl.stream()
                    .forEach(tr->{
                        double tradeCurrentProfitPercentage = (tr.getUnrealizedPL().doubleValue() / tr.getInitialMarginRequired().doubleValue()) * 100.0;
                        log.info("OpenTrade|{}|Unrealized Profit&Loss : {}|Current Profit Percentage : {}|Expected Profit Percentage : {}",tr.getInstrument(),tr.getUnrealizedPL().doubleValue(),tradeCurrentProfitPercentage,appConfigProperties.getBroker().getMinTradeProfitPercentage());
                        TradeInstrument ti = portfolioStatus.getTradeInstrumentMap().get(tr.getInstrument());
                        if(tradeCurrentProfitPercentage >= appConfigProperties.getBroker().getExtremeTradeProfitPercentage()){
                            ti.setHasExtremeEnd(true);
                        }
                        if(isTrendReversalWithLoss(ti,tr) || isExtremeEndReversal(ti,tradeCurrentProfitPercentage) || isIndicatorReversalWithProfit(ti,tradeCurrentProfitPercentage,tr)){
                            tradeService.closeTradeForCurrentAccount(tr.getId());
                            updateLastTradeCloseSignal(ti);
                            ti.setHasExtremeEnd(false);
                        }
                    });
        });
    }

    private boolean isExtremeEndReversal(TradeInstrument ti,double tradeCurrentProfitPercentage){
        return ti.getHasExtremeEnd() && tradeCurrentProfitPercentage > 0 && tradeCurrentProfitPercentage < appConfigProperties.getBroker().getCutoffTradeProfitPercentage();
    }

    private boolean isIndicatorReversalWithProfit(TradeInstrument ti,double tradeCurrentProfitPercentage,Trade tr){
        return tradeCurrentProfitPercentage > appConfigProperties.getBroker().getMinTradeProfitPercentage() && ((getUpperScreenTrend(ti) > 0 && tr.getCurrentUnits().doubleValue() > 0 && tradeClosableStatus(ti) < 0) || (getUpperScreenTrend(ti) < 0 && tr.getCurrentUnits().doubleValue() < 0 && tradeClosableStatus(ti) > 0));
    }

    private boolean isTrendReversalWithLoss(TradeInstrument ti,Trade tr){
        return (getUpperScreenTrendWhenReversal(ti) > 0 && tr.getCurrentUnits().doubleValue() < 0) || (getUpperScreenTrendWhenReversal(ti) < 0 && tr.getCurrentUnits().doubleValue() > 0);
    }

    private int getUpperScreenTrend(TradeInstrument ti){
        if(ti.getCurrentAdxHighIndicator().getADX() > 25 && ti.getCurrentAdxHighIndicator().getMinusDI() > ti.getCurrentAdxHighIndicator().getPlusDI() && ti.getCurrentEmaHighIndicator().getSlowEMA() > ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() < ti.getCurrentStochasticHighIndicator().getDnP())
            return -1;
        else if(ti.getCurrentAdxHighIndicator().getADX() > 25 && ti.getCurrentAdxHighIndicator().getPlusDI() > ti.getCurrentAdxHighIndicator().getMinusDI() && ti.getCurrentEmaHighIndicator().getSlowEMA() < ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() > ti.getCurrentStochasticHighIndicator().getDnP())
            return 1;
        else
            return 0;
    }

    private int getUpperScreenTrendWhenReversal(TradeInstrument ti){
        if(ti.getCurrentAdxHighIndicator().getADX() > 25 && ti.getCurrentAdxHighIndicator().getMinusDI() > ti.getCurrentAdxHighIndicator().getPlusDI() && ti.getCurrentEmaHighIndicator().getSlowEMA() > ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() < ti.getCurrentStochasticHighIndicator().getDnP() && ti.getCurrentStochasticHighIndicator().getKP() < ti.getCurrentStochasticHighIndicator().getDP())
            return -1;
        else if(ti.getCurrentAdxHighIndicator().getADX() > 25 && ti.getCurrentAdxHighIndicator().getPlusDI() > ti.getCurrentAdxHighIndicator().getMinusDI() && ti.getCurrentEmaHighIndicator().getSlowEMA() < ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() > ti.getCurrentStochasticHighIndicator().getDnP() && ti.getCurrentStochasticHighIndicator().getKP() > ti.getCurrentStochasticHighIndicator().getDP())
            return 1;
        else
            return 0;
    }

    private int tradeClosableStatus(TradeInstrument ti){
        if(ti.getCurrentStochasticHighIndicator().getKP() < ti.getCurrentStochasticHighIndicator().getDP())
            return -1;
        if(ti.getCurrentStochasticHighIndicator().getKP() > ti.getCurrentStochasticHighIndicator().getDP())
            return 1;
        else
            return 0;
    }

    private int tradeEnterStatus(TradeInstrument ti){
        if(ti.getCurrentAdxLowIndicator().getADX() > 25 && ti.getCurrentAdxLowIndicator().getMinusDI() > ti.getCurrentAdxLowIndicator().getPlusDI() && ti.getCurrentStochasticLowIndicator().getKP() < ti.getCurrentStochasticLowIndicator().getDP())
            return -1;
        if(ti.getCurrentAdxLowIndicator().getADX() > 25 && ti.getCurrentAdxLowIndicator().getPlusDI() > ti.getCurrentAdxLowIndicator().getMinusDI() &&ti.getCurrentStochasticLowIndicator().getKP() > ti.getCurrentStochasticLowIndicator().getDP())
            return 1;
        else
            return 0;
    }

    private int tradeOpeningStatus(TradeInstrument ti){
        if(getUpperScreenTrend(ti) > 0 && tradeEnterStatus(ti) > 0 && ti.getCurrentStochasticLowIndicator().getTouchedBelow())
            return 1;
        else if(getUpperScreenTrend(ti) < 0 && tradeEnterStatus(ti) < 0 && ti.getCurrentStochasticLowIndicator().getTouchAbove())
            return -1;
        else
            return 0;
    }

    private boolean tradeOpeningAvailability(TradeInstrument ti){
        int status = tradeOpeningStatus(ti);
        return status > 0 || status < 0;
    }

    private void openEligibleTrades(){
        if(Boolean.FALSE.equals(appConfigProperties.getBroker().getSkipFutureTrades())){
            log.info("Further trading has been turned on");
            List<InstrumentName> openTradeInstruments = tradeService.getOpenTradesForCurrentAccount().map(trl -> trl.stream().map(tr -> tr.getInstrument()).collect(Collectors.toList())).orElse(null);
            List<TradeInstrument> tradableInstruments;
            if(openTradeInstruments != null && !openTradeInstruments.isEmpty()){
                tradableInstruments = portfolioStatus.getTradeInstrumentMap().values().stream()
                        .filter(ti -> !openTradeInstruments.contains(ti.getInstrument().getName()))
                        .filter(ti->tradeOpeningAvailability(ti))
                        .collect(Collectors.toList());
            }
            else{
                tradableInstruments = portfolioStatus.getTradeInstrumentMap().values().stream().filter(ti->tradeOpeningAvailability(ti)).collect(Collectors.toList());
            }
            if(tradableInstruments != null && !tradableInstruments.isEmpty()){
                AppUtil.updateAllCurrentFraction(tradableInstruments);
                AppUtil.updateAllMaxUnitCount(tradableInstruments,portfolioStatus);
                openAllEligibleTrades(tradableInstruments);
            }
        }
        else{
            log.info("Further trading has been turned off");
        }

    }

    private void openAllEligibleTrades(List<TradeInstrument> tradableInstruments){
        tradableInstruments.stream().forEach(ti->{
            if(tradeOpeningStatus(ti) > 0){
                log.info("Opening a buy order with total fraction for {} is {}",ti.getInstrument().getName(),ti.getCurrentFraction());
                OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * 1.0);
                ti.getCurrentStochasticLowIndicator().resetLevels();
                ti.setLastTradeCloseSignal(0);
                ti.incrementOpenedTradeCount();
            }
            else if(tradeOpeningStatus(ti) < 0){
                log.info("Opening a sell order with total fraction for {} is {}",ti.getInstrument().getName(),ti.getCurrentFraction());
                OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * -1.0);
                ti.getCurrentStochasticLowIndicator().resetLevels();
                ti.setLastTradeCloseSignal(0);
                ti.incrementOpenedTradeCount();
            }
        });
    }
}
