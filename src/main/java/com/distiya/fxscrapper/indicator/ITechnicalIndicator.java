package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.PredictedCandle;
import com.oanda.v20.instrument.CandlestickData;

public interface ITechnicalIndicator {
    void update(PredictedCandle currentPrice);
    void update(CandlestickData currentPrice);
}
