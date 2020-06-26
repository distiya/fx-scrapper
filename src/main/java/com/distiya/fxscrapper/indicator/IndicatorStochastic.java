package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.Candle;
import com.distiya.fxscrapper.predict.PredictedCandle;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.primitives.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class IndicatorStochastic implements ITechnicalIndicator{

    private Integer k = 10;
    private Integer d = 6;
    private Integer dn = 6;
    private Integer count = 0;
    List<PredictedCandle> candleHistory = new LinkedList<>();
    @Getter
    private Boolean touchedBelow = Boolean.FALSE;
    @Getter
    private Boolean touchAbove = Boolean.FALSE;
    @Getter
    private Double kP = 0.0;
    @Getter
    private Double dP = 0.0;
    @Getter
    private Double dnP = 0.0;
    private String identifier = "";
    @Setter
    private Instrument instrument;

    public IndicatorStochastic(Integer k,Integer d,Integer dn,String identifier,Instrument instrument){
        this.k = k;
        this.d = d;
        this.dn = dn;
        this.identifier = identifier;
        this.instrument = instrument;
    }

    @Override
    public void update(PredictedCandle currentPrice) {
        if(candleHistory.size() == k){
            candleHistory.remove(0);
            candleHistory.add(currentPrice);
            Double previousDP = dP;
            Double previousDNP = dnP;
            kP = getCurrentStochastic(currentPrice.getPredicted().getClose(),calculateCurrentHigh(),calculateCurrentLow());
            dP = getCurrentMA(d,kP,previousDP);
            dnP = getCurrentMA(dn,dP,previousDNP);
            log.info("{}|{}|KP : {}, DP : {}, DNP : {}",this.instrument.getName(),this.identifier,this.kP,this.dP,this.dnP);
            if(kP < 35)
                touchedBelow = Boolean.TRUE;
            else if(kP > 65)
                touchAbove = Boolean.TRUE;
            if(count > 0 && count % 3 == 0)
                resetLevels();
            else
                count++;
        }
        else
            candleHistory.add(currentPrice);
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

    public void resetLevels(){
        touchAbove = Boolean.FALSE;
        touchedBelow = Boolean.FALSE;
        count = 0;
    }

    private double getCurrentMA(double period,double currentValue,double previousMA){
        return previousMA + (currentValue - previousMA)/period;
    }

    private double calculateCurrentHigh(){
        return candleHistory.stream().mapToDouble(pc->pc.getPredicted().getHigh()).summaryStatistics().getMax();
    }

    private double calculateCurrentLow(){
        return candleHistory.stream().mapToDouble(pc->pc.getPredicted().getLow()).summaryStatistics().getMin();
    }

    private double getCurrentStochastic(double close,double high, double low){
        return (close - low)/(high - low)*100;
    }
}
