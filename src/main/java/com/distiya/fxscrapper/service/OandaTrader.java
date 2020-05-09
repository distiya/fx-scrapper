package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.Candlestick;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
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
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Autowired
    private PortfolioStatus portfolioStatus;

    @Scheduled(cron = "${app.config.broker.orderPlacing}")
    public void trade(){
        portfolioStatus.setAccount(accountService.getCurrentAccount().orElse(portfolioStatus.getAccount()));
        portfolioStatus.setMargin(portfolioStatus.getAccount().getMarginAvailable().doubleValue());
        updateAllMarketCandles(2l);
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
            tradeInstrument.setPreviousMarket(cl.get(0).getMid());
            tradeInstrument.setCurrentMarket(cl.get(1).getMid());
        }
    }
}
