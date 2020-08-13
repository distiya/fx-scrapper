package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.properties.SupportedTickerProperties;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.InstrumentName;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IHistoryService {
    void requestHistory(SupportedTickerProperties ticker, SupportedResolutionProperties resolution, LocalDate startDate);
    Optional<List<Candlestick>> requestHistory(InstrumentName instrument, CandlestickGranularity granularity, long count);
}
