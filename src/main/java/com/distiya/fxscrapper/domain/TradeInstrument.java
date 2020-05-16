package com.distiya.fxscrapper.domain;

import com.distiya.fxscrapper.indicator.IndicatorEMA;
import com.distiya.fxscrapper.indicator.IndicatorStochastic;
import com.oanda.v20.instrument.Candlestick;
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
    private List<Candlestick> marketHistory;
    private CandlestickData previousPredicted;
    private CandlestickData currentPredicted;
    private double volume;
    private IndicatorEMA emaIndicator = new IndicatorEMA(12,5);
    private IndicatorStochastic stochasticIndicator = new IndicatorStochastic(10,6,6);
}
