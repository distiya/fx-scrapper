package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.BoundedLimitOrder;
import com.distiya.fxscrapper.domain.OrderType;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.order.*;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class OandaOrderService implements IOrderService{

    @Autowired
    private Context context;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Override
    public Optional<BoundedLimitOrder> placeLongLimitOrder(AccountID accountID, InstrumentName instrument, double units, double price) {
        TransactionID id = createOrder(accountID,instrument,units,price);
        if(id != null){
            BoundedLimitOrder boundedLimitOrder = new BoundedLimitOrder();
            boundedLimitOrder.setOrderType(OrderType.LONG_LIMIT);
            boundedLimitOrder.setLongOrderId(id);
            boundedLimitOrder.setUnits(units);
            boundedLimitOrder.setLongPrice(price);
            log.info("Long Limit Order Created : {}",id.toString());
            return Optional.of(boundedLimitOrder);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BoundedLimitOrder> placeShortLimitOrder(AccountID accountID, InstrumentName instrument, double units, double price) {
        TransactionID id = createOrder(accountID,instrument,-1*units,price);
        if(id != null){
            BoundedLimitOrder boundedLimitOrder = new BoundedLimitOrder();
            boundedLimitOrder.setOrderType(OrderType.SHORT_LIMIT);
            boundedLimitOrder.setShortOrderId(id);
            boundedLimitOrder.setUnits(units);
            boundedLimitOrder.setShortPrice(price);
            log.info("Short Limit Order Created : {}",id.toString());
            return Optional.of(boundedLimitOrder);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BoundedLimitOrder> placeBoundLimitOrder(AccountID accountID, InstrumentName instrument, double units, double longPrice, double shortPrice) {
        TransactionID longId = createOrder(accountID,instrument,units,longPrice);
        TransactionID shortId = createOrder(accountID,instrument,-1*units,shortPrice);
        if(longId != null && shortId != null){
            BoundedLimitOrder boundedLimitOrder = new BoundedLimitOrder();
            boundedLimitOrder.setOrderType(OrderType.BOUNDED_LIMIT);
            boundedLimitOrder.setLongOrderId(longId);
            boundedLimitOrder.setShortOrderId(shortId);
            boundedLimitOrder.setUnits(units);
            boundedLimitOrder.setLongPrice(longPrice);
            boundedLimitOrder.setShortPrice(shortPrice);
            log.info("Bounded Limit Order Created, Long Order Id : {} and Short Order Id : {}",longId.toString(),shortId.toString());
            return Optional.of(boundedLimitOrder);
        }
        else if(longId == null && shortId != null){
            cancelOrder(accountID,shortId);
        }
        else if(longId != null && shortId == null){
            cancelOrder(accountID,longId);
        }
        return Optional.empty();
    }

    @Override
    public boolean cancelOrder(AccountID accountID, TransactionID orderId) {
        OrderSpecifier orderSpecifier = new OrderSpecifier(orderId);
        try {
            context.order.cancel(accountID,orderSpecifier);
            return true;
        } catch (RequestException e) {
            log.error("RequestException while cancelling order | account:{},orderId:{},message:{}",accountID,toString(),orderId.toString(),e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while cancelling order | account:{},orderId:{},message:{}",accountID,toString(),orderId.toString(),e.getMessage());
        }
        return false;
    }

    @Override
    public Optional<Order> getOrder(AccountID accountID, TransactionID orderId) {
        OrderSpecifier os = new OrderSpecifier(orderId);
        try {
            return Optional.of(context.order.get(accountID, os).getOrder());
        } catch (RequestException e) {
            log.info("RequestException while getting order | account:{},orderId:{},message:{}",accountID.toString(),orderId.toString(),e.getErrorMessage());
        } catch (ExecuteException e) {
            log.info("ExecuteException while getting order | account:{},orderId:{},message:{}",accountID.toString(),orderId.toString(),e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<BoundedLimitOrder> placeLongLimitOrderForCurrentAccount(InstrumentName instrument, double units, double price) {
        return placeLongLimitOrder(currentAccount,instrument,units,price);
    }

    @Override
    public Optional<BoundedLimitOrder> placeShortLimitOrderForCurrentAccount(InstrumentName instrument, double units, double price) {
        return placeShortLimitOrder(currentAccount,instrument,units,price);
    }

    @Override
    public Optional<BoundedLimitOrder> placeBoundLimitOrderForCurrentAccount(InstrumentName instrument, double units, double longPrice, double shortPrice) {
        return placeBoundLimitOrder(currentAccount,instrument,units,longPrice,shortPrice);
    }

    @Override
    public boolean cancelOrderForCurrentUser(TransactionID orderId) {
        return cancelOrder(currentAccount,orderId);
    }

    @Override
    public Optional<Order> getOrderForCurrentAccount(TransactionID orderId) {
        return getOrder(currentAccount,orderId);
    }

    private TransactionID createOrder(AccountID accountID,InstrumentName instrument,double units,double price){
        OrderCreateRequest request = new OrderCreateRequest(accountID);
        LimitOrderRequest lor = new LimitOrderRequest();
        lor.setInstrument(instrument);
        lor.setUnits(units);
        lor.setTriggerCondition(OrderTriggerCondition.MID);
        lor.setPrice(price);
        request.setOrder(lor);
        OrderCreateResponse response = null;
        try {
            return context.order.create(request).getOrderCreateTransaction().getId();
        } catch (RequestException e) {
            log.error("RequestException while creating limit order | account:{},instrument:{},price:{},units:{},message:{}",accountID.toString(),instrument.toString(),price,units,e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while creating limit order | account:{},instrument:{},price:{},units:{},message:{}",accountID.toString(),instrument.toString(),price,units,e.getMessage());
        }
        return null;
    }
}
