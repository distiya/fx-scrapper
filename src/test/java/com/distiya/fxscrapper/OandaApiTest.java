package com.distiya.fxscrapper;

import com.distiya.fxscrapper.domain.BoundedLimitOrder;
import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.service.OandaHistoryService;
import com.distiya.fxscrapper.service.OandaOrderService;
import com.distiya.fxscrapper.service.OandaPricingService;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class OandaApiTest {

    @Autowired
    private OandaPricingService pricingService;

    @Autowired
    private OandaOrderService orderService;

    @Autowired
    private OandaHistoryService historyService;

    @Autowired
    private PortfolioStatus portfolioStatus;

    @Test
    public void sampleTest(){

    }

    @Test
    public void testCurrentPrices(){
        Optional<BoundedLimitOrder> usd_sgd = orderService.placeBoundLimitOrderForCurrentAccount(new InstrumentName("USD_SGD"), 5000.0, 1.4125, 1.4135);
    }

    @Test
    public void cancelOrder(){
        orderService.cancelOrderForCurrentUser(new TransactionID("19"));
    }

    @Test
    public void getCandles(){
        Optional<List<Candlestick>> usd_sgd = historyService.requestHistory(new InstrumentName("USD_SGD"), CandlestickGranularity.M5, 2);
        usd_sgd.ifPresent(l->{
            l.stream().forEach(c->{
                System.out.println(c.getTime().toString()+" : "+c.getMid().getC().doubleValue());
            });
        });
    }

    @Test
    public void testPortfolioStatus(){
        double margin = portfolioStatus.getMargin();
        System.out.println("Margin : "+margin);
    }
}
