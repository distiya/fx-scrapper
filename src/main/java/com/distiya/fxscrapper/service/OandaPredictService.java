package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.distiya.fxscrapper.predict.*;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.instrument.CandlestickGranularity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
public class OandaPredictService implements IPredictService{

    @Autowired
    private FxPredictGrpc.FxPredictBlockingStub predictClient;

    @Override
    public void getPredictionsForPortfolio(CandlestickGranularity granularity, PortfolioStatus portfolioStatus){
        MultiPredictBatchForGranularity.Builder multiPredictBatch = MultiPredictBatchForGranularity.newBuilder().setGranularity(granularity.name());
        List<PredictBatch> predictBatchList = portfolioStatus.getTradeInstrumentMap().values().stream().map(ti -> getPredictBatchForTradeInstrument(ti))
                .collect(Collectors.toList());
        multiPredictBatch.addAllBatches(predictBatchList);
        MultiPredictBatchForGranularity multiPredictBatchRequest = multiPredictBatch.build();
        MultiPredictedCandleForGranularity predictionForBatchResponse = predictClient.getPredictionForBatch(multiPredictBatchRequest);
        updatePortfolioWithNewPredictions(portfolioStatus.getTradeInstrumentMap(),predictionForBatchResponse);
    }

    @Override
    public void getPredictionsForPortfolio(PortfolioStatus portfolioStatus){
        getPredictionsForPortfolio(portfolioStatus.getTradingGranularity(),portfolioStatus);
    }

    private PredictBatch getPredictBatchForTradeInstrument(TradeInstrument ti){
        PredictBatch.Builder predictBatchBuilder = PredictBatch.newBuilder();
        predictBatchBuilder.setTicker(ti.getInstrument().getName().toString());
        predictBatchBuilder.addAllCandles(getCandleListForCurrentMarketHistory(ti.getMarketHistory()));
        return predictBatchBuilder.build();
    }
    private List<Candle> getCandleListForCurrentMarketHistory(List<CandlestickData> history){
        return history.stream().map(mh->
            Candle.newBuilder()
                    .setOpen(mh.getO().doubleValue())
                    .setClose(mh.getC().doubleValue())
                    .setHigh(mh.getH().doubleValue())
                    .setLow(mh.getL().doubleValue())
                    .build()
        ).collect(Collectors.toList());
    }

    private void updatePortfolioWithNewPredictions(Map<String, TradeInstrument> tradeInstrumentMap,MultiPredictedCandleForGranularity predictionForBatchResponse){
        predictionForBatchResponse.getCandlesList().stream().forEach(pc->{
            CandlestickData newPredict = new CandlestickData();
            newPredict.setO(pc.getPredicted().getOpen());
            newPredict.setC(pc.getPredicted().getClose());
            newPredict.setH(pc.getPredicted().getHigh());
            newPredict.setL(pc.getPredicted().getLow());
            TradeInstrument tradeInstrument = tradeInstrumentMap.get(pc.getTicker());
            tradeInstrument.setPreviousPredicted(tradeInstrument.getCurrentPredicted());
            tradeInstrument.setCurrentPredicted(newPredict);
        });
    }
}
