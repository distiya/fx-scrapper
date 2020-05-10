package com.distiya.fxscrapper;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.service.*;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    @Autowired
    private AccountID accountID;

    @Autowired
    private Context context;

    @Autowired
    private OandaStreamService streamService;

    @Autowired
    private OandaPredictService predictService;

    @Test
    public void sampleTest(){

    }

    /*@Test
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

    @Test
    public void testTransactionStream(){
        streamService.getTransaction()
                .subscribe(System.out::println);
    }*/

    @Test
    public void testPredictService(){
        predictService.getPredictionsForPortfolio(portfolioStatus);
    }
}
