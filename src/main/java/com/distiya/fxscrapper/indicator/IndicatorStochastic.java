package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.PredictedCandle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class IndicatorStochastic implements ITechnicalIndicator{

    private Integer k = 10;
    private Integer d = 6;
    private Integer dn = 6;
    List<PredictedCandle> candleHistory = new LinkedList<>();
    @Getter
    private Double kP = 0.0;
    @Getter
    private Double dP = 0.0;
    @Getter
    private Double dnP = 0.0;

    public IndicatorStochastic(Integer k,Integer d,Integer dn){
        this.k = k;
        this.d = d;
        this.dn = dn;
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
        }
        else
            candleHistory.add(currentPrice);
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
