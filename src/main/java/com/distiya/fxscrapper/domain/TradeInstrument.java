package com.distiya.fxscrapper.domain;

import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.primitives.Instrument;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TradeInstrument {
    private String ticker;
    private double currentPrice = 0;
    private double currentFraction = 0;
    private double maxUnits = 0;
    private Instrument instrument;
    private BoundedLimitOrder currentOrder;
    private CandlestickData previousMarket;
    private CandlestickData currentMarket;
    private List<CandlestickData> marketHistory;
    private CandlestickData previousPredicted;
    private CandlestickData currentPredicted;
}
