package com.distiya.fxscrapper.service;

import com.oanda.v20.account.AccountID;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeID;

import java.util.List;
import java.util.Optional;

public interface ITradeService {
    Optional<List<Trade>> getOpenTrades(AccountID accountID);
    Optional<List<Trade>> getOpenTradesForCurrentAccount();
    void closeTrade(AccountID accountID, TradeID tradeID);
    void closeTradeForCurrentAccount(TradeID tradeID);
}
