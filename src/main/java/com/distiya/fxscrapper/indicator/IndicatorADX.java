package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.Candle;
import com.distiya.fxscrapper.predict.PredictedCandle;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.primitives.Instrument;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class IndicatorADX implements ITechnicalIndicator {

    public IndicatorADX(int period,Instrument instrument,String identifier){
        this.p = period;
        this.instrument = instrument;
        this.identifier = identifier;
    }

    private int p = 14;
    private PredictedCandle previousCandle;
    private PredictedCandle currentCandle;
    private double atr = 0.0d;
    private int trc = 0;
    private int dic = 0;
    private int adc = 0;
    private boolean ready = false;
    private double plusDIEMA = 0.0d;
    private double minusDIEMA = 0.0d;
    private Double plusDI = 0.0d;
    private Double minusDI = 0.0d;
    private double adxEMA = 0.0d;
    private Double adx = 0.0d;
    private double emaFactor = 2.0/(p+1.0);
    private String identifier = "";
    @Setter
    private Instrument instrument;

    @Override
    public void update(PredictedCandle currentPrice) {
        previousCandle = currentCandle;
        currentCandle = currentPrice;
        if(previousCandle != null && currentCandle != null){
            // tr = max(high,previous close) - min(low,previous close)
            double tr = Math.max(currentCandle.getPredicted().getHigh(),previousCandle.getPredicted().getClose()) - Math.min(currentCandle.getPredicted().getLow(),previousCandle.getPredicted().getClose());
            if(trc < p){
                atr += tr/p;
                trc++;
            }
            else{
                calculateDIEMA(atr);
                atr = (atr*(p-1) + tr)/p;
            }
        }
        log.info("{}|{}|ADX : {}, DI+ : {}, DI- : {}",this.instrument.getName(),this.identifier,this.adx,this.plusDI,this.minusDI);
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

    public double getADX(){
        if(adc < p)
            return -1.0d;
        else
            return adx;
    }

    public double getPlusDI(){
        if(adc < p)
            return -1.0d;
        else
            return plusDI;
    }

    public double getMinusDI(){
        if(adc < p)
            return -1.0d;
        else
            return minusDI;
    }

    private void calculateDIEMA(double atr){
        double dm_plus = 0.0d;
        double dm_minus = 0.0d;
        double upm = currentCandle.getPredicted().getHigh() - previousCandle.getPredicted().getHigh();
        double dwm = previousCandle.getPredicted().getLow() - currentCandle.getPredicted().getLow();
        dm_plus = (upm > dwm && upm > 0) ? upm/atr : 0.0d;
        dm_minus = (dwm > upm && dwm > 0) ? dwm/atr : 0.0d;
        if(dic < p){
            plusDIEMA += dm_plus/p;
            minusDIEMA += dm_minus/p;
            dic++;
        }
        else{
            calculateDI();
            plusDIEMA = (dm_plus - plusDIEMA)*emaFactor+plusDIEMA;
            minusDIEMA = (dm_minus - minusDIEMA)*emaFactor+minusDIEMA;
        }
    }

    private void calculateDI(){
        plusDI = plusDIEMA * 100;
        minusDI = minusDIEMA * 100;
        if(adc<p){
            adxEMA += Math.abs((plusDI-minusDI)/(plusDI+minusDI))/p;
            adc++;
        }
        else{
            adx = 100*adxEMA;
            adxEMA = (Math.abs((plusDI-minusDI)/(plusDI+minusDI)) - adxEMA)*emaFactor+adxEMA;
        }
    }
}
