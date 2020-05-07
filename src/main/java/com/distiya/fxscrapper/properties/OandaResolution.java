package com.distiya.fxscrapper.properties;

import com.oanda.v20.instrument.CandlestickGranularity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

@NoArgsConstructor
@Setter
@Getter
public class OandaResolution {

    private CandlestickGranularity granularity;
    private UnaryOperator<LocalDateTime> increment;
    private UnaryOperator<LocalDateTime> decrement;

    public OandaResolution(SupportedResolutionProperties resolution){
        reInitiate(resolution);
    }

    public void reInitiate(SupportedResolutionProperties resolution){
        this.granularity = resolveProvidedResolution(resolution);
        this.increment = getIncrementTimeFunction(resolution);
        this.decrement = getDecrementTimeFunction(resolution);
    }

    private UnaryOperator<LocalDateTime> getIncrementTimeFunction(SupportedResolutionProperties reolution){
        UnaryOperator<LocalDateTime> tf = t->t.plusMinutes(reolution.getMinutes()).plusHours(reolution.getHours()).plusDays(reolution.getDays()).plusWeeks(reolution.getWeeks()).plusMonths(reolution.getMonths());
        return tf;
    }

    private UnaryOperator<LocalDateTime> getDecrementTimeFunction(SupportedResolutionProperties reolution){
        UnaryOperator<LocalDateTime> tf = t->t.minusMinutes(reolution.getMinutes()).minusHours(reolution.getHours()).minusDays(reolution.getDays()).minusWeeks(reolution.getWeeks()).minusMonths(reolution.getMonths());
        return tf;
    }

    private CandlestickGranularity resolveProvidedResolution(SupportedResolutionProperties reolution){
        switch(reolution.getSymbol()){
            case "M5":return CandlestickGranularity.M5;
            case "M10":return CandlestickGranularity.M10;
            case "M15":return CandlestickGranularity.M15;
            case "M30":return CandlestickGranularity.M30;
            case "H1":return CandlestickGranularity.H1;
            case "H2":return CandlestickGranularity.H2;
            case "H3":return CandlestickGranularity.H3;
            case "H4":return CandlestickGranularity.H4;
            case "H6":return CandlestickGranularity.H6;
            case "H8":return CandlestickGranularity.H8;
            case "H12":return CandlestickGranularity.H12;
            case "D":return CandlestickGranularity.D;
            case "W":return CandlestickGranularity.W;
            case "M":return CandlestickGranularity.M;
        }
        return CandlestickGranularity.M5;
    }
}
