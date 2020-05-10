package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.oanda.v20.instrument.CandlestickGranularity;

public interface IPredictService {
    void getPredictionsForPortfolio(CandlestickGranularity granularity, PortfolioStatus portfolioStatus);
    void getPredictionsForPortfolio(PortfolioStatus portfolioStatus);
}
