package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.Candle;
import com.distiya.fxscrapper.predict.PredictedCandle;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.primitives.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndicatorEMA implements ITechnicalIndicator{
    private Integer slowPeriod=12;
    private Integer fastPeriod=5;
    private Double slowFactor = 2.0/(slowPeriod+1.0);
    private Double fastFactor = 2.0/(fastPeriod+1.0);
    private Double previousSlowEMA = 0.0;
    private Double previousFastEMA = 0.0;
    @Getter
    private Integer currentSignal = 0;
    private String identifier = "";
    @Setter
    private Instrument instrument;

    public IndicatorEMA(Integer slowPeriod,Integer fastPeriod,String identifier,Instrument instrument){
        this.slowPeriod = slowPeriod;
        this.fastPeriod = fastPeriod;
        this.identifier = identifier;
        this.instrument = instrument;
    }
    @Override
    public void update(PredictedCandle currentPrice) {
        Double previousEMADiff = this.previousSlowEMA - this.previousFastEMA;
        this.previousSlowEMA = currentPrice.getPredicted().getClose() * this.slowFactor + this.previousSlowEMA * (1-this.slowFactor);
        this.previousFastEMA = currentPrice.getPredicted().getClose() * this.fastFactor + this.previousFastEMA * (1-this.fastFactor);
        log.info("{}|{}|Slow : {}, Fast : {}",this.instrument.getName(),this.identifier,this.previousSlowEMA,this.previousFastEMA);
        Double currentEMADiff = this.previousSlowEMA - this.previousFastEMA;
        if(previousEMADiff < 0 && currentEMADiff > 0)
            this.currentSignal = -1;
        else if(previousEMADiff > 0 && currentEMADiff < 0)
            this.currentSignal = 1;
        else
            this.currentSignal = 0;
    }

    @Override
    public void update(CandlestickData currentPrice) {
        Candle candle = Candle.newBuilder()
                .setOpen(currentPrice.getO().doubleValue())
                .setClose(currentPrice.getC().doubleValue())
                .setHigh(currentPrice.getH().doubleValue())
                .setLow(currentPrice.getL().doubleValue())
                .build();
        PredictedCandle predictedCandle = PredictedCandle.newBuilder().setPredicted(candle).build();
        update(predictedCandle);
    }

    public Double getSlowEMA(){
        return this.previousSlowEMA;
    }

    public Double getFastEMA(){
        return this.previousFastEMA;
    }
}
