package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.BoundedLimitOrder;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.order.Order;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;

import java.util.List;
import java.util.Optional;

public interface IOrderService {
    Optional<BoundedLimitOrder> placeLongLimitOrder(AccountID accountID,InstrumentName instrument, double units, double price);
    Optional<BoundedLimitOrder> placeShortLimitOrder(AccountID accountID,InstrumentName instrument,double units,double price);
    Optional<BoundedLimitOrder> placeBoundLimitOrder(AccountID accountID,InstrumentName instrument,double units,double longPrice,double shortPrice);
    boolean cancelOrder(AccountID accountID, TransactionID orderId);
    Optional<Order> getOrder(AccountID accountID,TransactionID orderId);
    Optional<List<Order>> getOpenOrders(AccountID accountID);
    OrderCreateResponse placeMarketOrder(AccountID accountID, InstrumentName instrument, double units);

    Optional<BoundedLimitOrder> placeLongLimitOrderForCurrentAccount(InstrumentName instrument, double units, double price);
    Optional<BoundedLimitOrder> placeShortLimitOrderForCurrentAccount(InstrumentName instrument,double units,double price);
    Optional<BoundedLimitOrder> placeBoundLimitOrderForCurrentAccount(InstrumentName instrument,double units,double longPrice,double shortPrice);
    boolean cancelOrderForCurrentUser(TransactionID orderId);
    Optional<Order> getOrderForCurrentAccount(TransactionID orderId);
    Optional<List<Order>> getOpenOrdersForCurrentAccount();
    OrderCreateResponse placeMarketOrderForCurrentAccount(InstrumentName instrument, double units);
}
