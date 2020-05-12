package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeID;
import com.oanda.v20.trade.TradeSpecifier;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
public class OandaTradeService implements ITradeService{

    @Autowired
    private Context context;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    public Optional<List<Trade>> getOpenTrades(AccountID accountID){
        try {
            return Optional.of(context.trade.listOpen(accountID).getTrades());
        } catch (RequestException e) {
            log.error("RequestException while getting open trades | account:{}",accountID,e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while getting open trades | account:{}",accountID,e.getMessage());
        }
        return Optional.empty();
    }

    public void closeTrade(AccountID accountID, TradeID tradeID){
        TradeSpecifier tradeSpecifier = new TradeSpecifier(tradeID);
        TradeCloseRequest tradeCloseRequest = new TradeCloseRequest(accountID,tradeSpecifier);
        try {
            context.trade.close(tradeCloseRequest);
            log.info("Trade {} closed for account {}",tradeID,accountID);
        } catch (RequestException e) {
            log.error("RequestException while closing trades | account:{},tradeId:{}",accountID,tradeID,e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("RequestException while closing trades | account:{},tradeId:{}",accountID,tradeID,e.getMessage());
        }
    }

    public void closeTradeForCurrentAccount(TradeID tradeID){
        closeTrade(currentAccount,tradeID);
    }

    public Optional<List<Trade>> getOpenTradesForCurrentAccount(){
        return getOpenTrades(currentAccount);
    }
}
