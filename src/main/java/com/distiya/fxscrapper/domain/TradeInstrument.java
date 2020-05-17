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
    private CandlestickData previousLowMarket;
    private CandlestickData currentLowMarket;
    private CandlestickData previousHighMarket;
    private CandlestickData currentHighMarket;
    private List<Candlestick> lowTimeMarketHistory;
    private List<Candlestick> highTimeMarketHistory;
    private CandlestickData previousLowPredicted;
    private CandlestickData currentLowPredicted;
    private CandlestickData previousHighPredicted;
    private CandlestickData currentHighPredicted;
    private double dailyVolume = 0;
    private Integer tradeDirection = 0;
    private IndicatorEMA emaLowIndicator = new IndicatorEMA(12,5);
    private IndicatorStochastic stochasticLowIndicator = new IndicatorStochastic(10,6,6);
    private IndicatorEMA emaHighIndicator = new IndicatorEMA(12,5);
    private IndicatorStochastic stochasticHighIndicator = new IndicatorStochastic(10,6,6);
}
