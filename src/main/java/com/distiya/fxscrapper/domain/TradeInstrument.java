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
    private IndicatorEMA emaLowIndicator = new IndicatorEMA(12,5,"PredictedLowScreenEMA",instrument);
    private IndicatorStochastic stochasticLowIndicator = new IndicatorStochastic(10,6,6,"PredictedLowScreenStochastic",instrument);
    private IndicatorEMA emaHighIndicator = new IndicatorEMA(12,5,"PredictedHighScreenEMA",instrument);
    private IndicatorStochastic stochasticHighIndicator = new IndicatorStochastic(10,6,6,"PredictedHighScreenStochastic",instrument);
    private IndicatorEMA currentEmaLowIndicator = new IndicatorEMA(12,5,"CurrentLowScreenEMA",instrument);
    private IndicatorStochastic currentStochasticLowIndicator = new IndicatorStochastic(10,6,6,"CurrentLowScreenStochastic",instrument);
    private IndicatorEMA currentEmaHighIndicator = new IndicatorEMA(12,5,"CurrentHighScreenEMA",instrument);
    private IndicatorStochastic currentStochasticHighIndicator = new IndicatorStochastic(10,6,6,"CurrentHighScreenStochastic",instrument);

    public void updateIndicators(){
        emaLowIndicator.setInstrument(instrument);
        stochasticLowIndicator.setInstrument(instrument);
        emaHighIndicator.setInstrument(instrument);
        stochasticHighIndicator.setInstrument(instrument);
        currentEmaLowIndicator.setInstrument(instrument);
        currentStochasticLowIndicator.setInstrument(instrument);
        currentEmaHighIndicator.setInstrument(instrument);
        currentStochasticHighIndicator.setInstrument(instrument);
    }
}
