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
        closeEligibleTrades();
        openEligibleTrades();
    }

    private void closeEligibleTrades(){
        Optional<List<Trade>> currentOpenTrades = tradeService.getOpenTradesForCurrentAccount();
        currentOpenTrades.ifPresent(otl->{
            otl.stream()
                    .forEach(tr->{
                        log.info("OpenTrade|{}|UPL : {}|RPL : {}|IMR : {}",tr.getInstrument(),tr.getUnrealizedPL().doubleValue(),tr.getRealizedPL().doubleValue(),tr.getInitialMarginRequired().doubleValue());
                        TradeInstrument ti = portfolioStatus.getTradeInstrumentMap().get(tr.getInstrument());
                        double tradeCurrentProfitPercentage = (tr.getUnrealizedPL().doubleValue() / tr.getInitialMarginRequired().doubleValue()) * 100.0;
                        if(tradeCurrentProfitPercentage > appConfigProperties.getBroker().getMinTradeProfitPercentage() && ((getUpperScreenTrend(ti) > 0 && tr.getCurrentUnits().doubleValue() > 0 && tradeClosableStatus(ti) < 0) || (getUpperScreenTrend(ti) < 0 && tr.getCurrentUnits().doubleValue() < 0 && tradeClosableStatus(ti) > 0))){
                            tradeService.closeTradeForCurrentAccount(tr.getId());
                        }
                    });
        });
    }

    private int getUpperScreenTrend(TradeInstrument ti){
        if(ti.getCurrentEmaHighIndicator().getSlowEMA() > ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() < ti.getCurrentStochasticHighIndicator().getDnP())
            return -1;
        else if(ti.getCurrentEmaHighIndicator().getSlowEMA() < ti.getCurrentEmaHighIndicator().getFastEMA() && ti.getCurrentStochasticHighIndicator().getDP() > ti.getCurrentStochasticHighIndicator().getDnP())
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
        if(ti.getCurrentStochasticLowIndicator().getKP() < ti.getCurrentStochasticLowIndicator().getDP())
            return -1;
        if(ti.getCurrentStochasticLowIndicator().getKP() > ti.getCurrentStochasticLowIndicator().getDP())
            return 1;
        else
            return 0;
    }

    private int tradeOpeningStatus(TradeInstrument ti){
        if(getUpperScreenTrend(ti) > 1 && tradeEnterStatus(ti) > 1 && ti.getCurrentStochasticLowIndicator().getTouchedBelow())
            return 1;
        else if(getUpperScreenTrend(ti) < 1 && tradeEnterStatus(ti) < 1 && ti.getCurrentStochasticLowIndicator().getTouchAbove())
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
    }

    private void openAllEligibleTrades(List<TradeInstrument> tradableInstruments){
        tradableInstruments.stream().forEach(ti->{
            if(tradeOpeningStatus(ti) > 0){
                log.info("Opening a buy order with total fraction for {} is {}",ti.getInstrument().getName(),ti.getCurrentFraction());
                OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * 1.0);
                ti.getCurrentStochasticLowIndicator().resetLevels();
            }
            else if(tradeOpeningStatus(ti) < 0){
                log.info("Opening a sell order with total fraction for {} is {}",ti.getInstrument().getName(),ti.getCurrentFraction());
                OrderCreateResponse orderCreateResponse = orderService.placeMarketOrderForCurrentAccount(ti.getInstrument().getName(), Math.floor(ti.getMaxUnits()) * -1.0);
                ti.getCurrentStochasticLowIndicator().resetLevels();
            }
        });
    }
}
