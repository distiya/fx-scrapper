package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.PredictedCandle;
import lombok.Getter;
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

    public IndicatorEMA(Integer slowPeriod,Integer fastPeriod){
        this.slowPeriod = slowPeriod;
        this.fastPeriod = fastPeriod;
    }
    @Override
    public void update(PredictedCandle currentPrice) {
        Double previousEMADiff = this.previousSlowEMA - this.previousFastEMA;
        this.previousSlowEMA = currentPrice.getPredicted().getClose() * this.slowFactor + this.previousSlowEMA * (1-this.slowFactor);
        this.previousFastEMA = currentPrice.getPredicted().getClose() * this.fastFactor + this.previousFastEMA * (1-this.fastFactor);
        Double currentEMADiff = this.previousSlowEMA - this.previousFastEMA;
        if(previousEMADiff < 0 && currentEMADiff > 0)
            this.currentSignal = -1;
        else if(previousEMADiff > 0 && currentEMADiff < 0)
            this.currentSignal = 1;
        else
            this.currentSignal = 0;
    }

    public Double getSlowEMA(){
        return this.previousSlowEMA;
    }

    public Double getFastEMA(){
        return this.previousFastEMA;
    }
}
