package com.distiya.fxscrapper.domain;

import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.transaction.TransactionID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoundedLimitOrder{
    InstrumentName instrument;
    double units = 0;
    private OrderType orderType;
    private TransactionID longOrderId;
    private TransactionID shortOrderId;
    private Integer consumedDirection = 0;
}
