package com.distiya.fxscrapper.properties;

import com.distiya.fxscrapper.util.AppUtil;
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
        UnaryOperator<LocalDateTime> tf = t->t.plusSeconds(reolution.getSeconds()).plusMinutes(reolution.getMinutes()).plusHours(reolution.getHours()).plusDays(reolution.getDays()).plusWeeks(reolution.getWeeks()).plusMonths(reolution.getMonths());
        return tf;
    }

    private UnaryOperator<LocalDateTime> getDecrementTimeFunction(SupportedResolutionProperties reolution){
        UnaryOperator<LocalDateTime> tf = t->t.minusSeconds(reolution.getSeconds()).minusMinutes(reolution.getMinutes()).minusHours(reolution.getHours()).minusDays(reolution.getDays()).minusWeeks(reolution.getWeeks()).minusMonths(reolution.getMonths());
        return tf;
    }

    private CandlestickGranularity resolveProvidedResolution(SupportedResolutionProperties reolution){
        return AppUtil.getCandlestickGranularity(reolution.getSymbol());
    }
}
