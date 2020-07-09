package com.distiya.fxscrapper.domain;

import com.distiya.fxscrapper.indicator.IndicatorADX;
import com.distiya.fxscrapper.indicator.IndicatorEMA;
import com.distiya.fxscrapper.indicator.IndicatorStochastic;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.primitives.DateTime;
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
    private DateTime lastLowCandleUpdatedTime;
    private DateTime lastHighCandleUpdatedTime;
    private double dailyVolume = 0;
    private Long openedTradeCount = 0l;
    private Integer tradeDirection = 0;
    private Integer lastTradeCloseSignal = 0;
    private Boolean hasExtremeEnd = false;
    private IndicatorEMA emaLowIndicator = new IndicatorEMA(12,5,"PredictedLowScreenEMA",instrument);
    private IndicatorStochastic stochasticLowIndicator = new IndicatorStochastic(10,6,6,"PredictedLowScreenStochastic",instrument);
    private IndicatorEMA emaHighIndicator = new IndicatorEMA(12,5,"PredictedHighScreenEMA",instrument);
    private IndicatorStochastic stochasticHighIndicator = new IndicatorStochastic(10,6,6,"PredictedHighScreenStochastic",instrument);
    private IndicatorEMA currentEmaLowIndicator = new IndicatorEMA(12,5,"CurrentLowScreenEMA",instrument);
    private IndicatorStochastic currentStochasticLowIndicator = new IndicatorStochastic(10,6,6,"CurrentLowScreenStochastic",instrument);
    private IndicatorStochastic exCurrentStochasticLowIndicator = new IndicatorStochastic(10,6,6,"CurrentLowScreenStochastic",instrument);
    private IndicatorEMA currentEmaHighIndicator = new IndicatorEMA(12,5,"CurrentHighScreenEMA",instrument);
    private IndicatorStochastic currentStochasticHighIndicator = new IndicatorStochastic(10,6,6,"CurrentHighScreenStochastic",instrument);
    private IndicatorStochastic exCurrentStochasticHighIndicator = new IndicatorStochastic(10,6,6,"CurrentHighScreenStochastic",instrument);
    private IndicatorADX currentAdxLowIndicator = new IndicatorADX(14,instrument,"CurrentLowADX");
    private IndicatorADX currentAdxHighIndicator = new IndicatorADX(14,instrument,"CurrentHighADX");

    public void updateIndicators(){
        emaLowIndicator.setInstrument(instrument);
        stochasticLowIndicator.setInstrument(instrument);
        emaHighIndicator.setInstrument(instrument);
        stochasticHighIndicator.setInstrument(instrument);
        currentEmaLowIndicator.setInstrument(instrument);
        currentStochasticLowIndicator.setInstrument(instrument);
        currentEmaHighIndicator.setInstrument(instrument);
        currentStochasticHighIndicator.setInstrument(instrument);
        currentAdxHighIndicator.setInstrument(instrument);
        currentAdxLowIndicator.setInstrument(instrument);
    }

    public void incrementOpenedTradeCount(){
        if(Long.MAX_VALUE == openedTradeCount.longValue())
            openedTradeCount = 1l;
        else
            openedTradeCount ++;
    }
}
