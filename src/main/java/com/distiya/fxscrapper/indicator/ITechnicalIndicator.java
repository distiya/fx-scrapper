package com.distiya.fxscrapper.indicator;

import com.distiya.fxscrapper.predict.PredictedCandle;

public interface ITechnicalIndicator {
    void update(PredictedCandle currentPrice);
}
