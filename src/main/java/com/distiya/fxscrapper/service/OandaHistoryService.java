package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.properties.SupportedTickerProperties;
import com.distiya.fxscrapper.request.CandleHistoryRequest;
import com.distiya.fxscrapper.util.AppUtil;
import com.distiya.trading.fxindicator.constant.IndicatorLogicApplier;
import com.distiya.trading.fxindicator.dto.CandleData;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.primitives.InstrumentName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.distiya.trading.fxindicator.constant.IndicatorLogicApplier.*;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
public class OandaHistoryService implements IHistoryService{

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private Context context;

    @Override
    public void requestHistory(SupportedTickerProperties ticker, SupportedResolutionProperties resolution, LocalDate startDate){
        CandleHistoryRequest candleHistoryRequest = new CandleHistoryRequest(appConfigProperties,ticker.getTicker(),resolution,startDate);
        Path path = Paths.get(candleHistoryRequest.getDataFileName());
        File dataFile = path.toFile();
        TechnicalIndicators technicalIndicators = new TechnicalIndicators(ticker);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(dataFile))){
            bw.write(technicalIndicators.generateColumnHeader());
            log.info("Data file writing started for {}",candleHistoryRequest.getDataFileName());
            requestHistoryForRound(candleHistoryRequest,bw,technicalIndicators);
            log.info("Data file writing ended for {}",candleHistoryRequest.getDataFileName());
        } catch (IOException e) {
            log.error("IOException while opening data file to write data {}",e.getMessage());
        }
    }

    @Override
    public Optional<List<Candlestick>> requestHistory(InstrumentName instrument, CandlestickGranularity granularity, long count){
        InstrumentCandlesRequest request = new InstrumentCandlesRequest(instrument);
        request.setCount(count);
        request.setGranularity(granularity);
        request.setPrice(appConfigProperties.getBroker().getCandleMode());
        request.setDailyAlignment(6l);
        request.setAlignmentTimezone(appConfigProperties.getBroker().getTimeZone());
        try {
            return Optional.of(context.instrument.candles(request).getCandles());
        } catch (RequestException e) {
            log.error("RequestException while requesting candles for instrument {} with granularity {} and count {} : {}",instrument,granularity,count,e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while requesting candles for instrument {} with granularity {} and count {} : {}",instrument,granularity,count,e.getMessage());
        }
        return Optional.empty();
    }

    private void requestHistoryForRound(CandleHistoryRequest candleHistoryRequest,BufferedWriter bw,TechnicalIndicators technicalIndicators) {
        try {
            InstrumentCandlesResponse candleResponse = context.instrument.candles(candleHistoryRequest.getRequest());
            if(!candleResponse.getCandles().isEmpty()){
                candleResponse.getCandles().stream().forEach(c->{
                    technicalIndicators.update(AppUtil.convertToLocalDateTime(c.getTime().toString()),c.getMid().getO().doubleValue(),c.getMid().getC().doubleValue(),c.getMid().getH().doubleValue(),c.getMid().getL().doubleValue());
                    String record = technicalIndicators.generateRow(AppUtil.convertToLocalDateTime(c.getTime().toString()),c.getMid().getO().doubleValue(),c.getMid().getC().doubleValue(),c.getMid().getH().doubleValue(),c.getMid().getL().doubleValue());
                    try {
                        bw.write(record);
                    } catch (IOException e) {
                        log.error("IOException in writing data record {}",e.getMessage());
                    }
                });
                candleHistoryRequest.updateFromTime(candleHistoryRequest.getOrs().getIncrement().apply(AppUtil.convertToLocalDateTime(candleResponse.getCandles().get(candleResponse.getCandles().size()-1).getTime().toString())));
                requestHistoryForRound(candleHistoryRequest,bw,technicalIndicators);
            }
        } catch (RequestException e) {
            log.error("RequestException {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException {}",e.getMessage());
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private class TechnicalIndicators{

        private SupportedTickerProperties ticker;
        private int adxValues[] = new int[]{5,10,14,17,21,30,37,42,47,52,65,80,92,104,115,125,128,140,150,165,180,200,230,256,270,290,320,350,370,400};
        private String adxColumns[] = new String[]{"adx","plusDI","minusDI"};
        private String crossEMADiffColumns[] = new String[]{"slow","fast"};
        private String stochasticColumns[] = new String[]{"kp","dp","dnp"};
        private int crossEMADiffSlowValues[] = new int[]{30,47,78,120,180};
        private int crossEMADiffFastValues[] = new int[]{5,10,12,15,17,19,20,22,25,28};
        private int rocValues[] = new int[]{5,10,14,17,21,30,37,42,47,52,65,80,92,104,115,125,128,140,150,165,180,200,230,256,270,290,320,350,370,400};
        private int rsiValues[] = new int[]{5,10,14,17,21,30,37,42,47,52,65,80,92,104,115,125,128,140,150,165,180,200,230,256,270,290,320,350,370,400};
        private int stochasticKPValues[] = new int[]{300,250,230,200,180,150,140,130,120,100,80,60,50,30,20,15,12,10};
        private int stochasticDPValues[] = new int[]{250,230,200,180,150,140,130,120,100,80,60,50,30,20,15,12,10,6};
        private int stochasticDnPValues[] = new int[]{250,230,200,180,150,140,130,120,100,80,60,50,30,20,15,12,10,6};
        private IndicatorLogicApplier valueSelector[] = new IndicatorLogicApplier[]{FOR_OPEN,FOR_CLOSE,FOR_HIGH,FOR_LOW};

        private LinkedList<IndicatorADX> adxIndicators = new LinkedList<>();
        private LinkedList<IndicatorCrossEMADiffPercent> crossEMADiffIndicators = new LinkedList<>();
        private LinkedList<IndicatorROC> rocIndicators = new LinkedList<>();
        private LinkedList<IndicatorRSI> rsiIndicators = new LinkedList<>();
        private LinkedList<IndicatorStochastic> stochasticIndicators = new LinkedList<>();

        private StringBuilder builder = new StringBuilder();

        TechnicalIndicators(SupportedTickerProperties ticker){
            this.ticker = ticker;
            createIndicators();
        }

        public String generateColumnHeader(){
            builder.delete(0,builder.length());
            builder.append("time,open,close,high,low,");
            Arrays.stream(adxValues).forEach(period->{
                Arrays.stream(adxColumns).forEach(column->{
                    builder.append("ADX_");
                    builder.append(period);
                    builder.append("_");
                    builder.append(column);
                    builder.append(",");
                });
            });

            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(crossEMADiffSlowValues).forEach(slow->{
                    Arrays.stream(crossEMADiffFastValues).forEach(fast->{
                        Arrays.stream(crossEMADiffColumns).forEach(prefix->{
                            builder.append("emadiff_");
                            builder.append(mapIndicatorLogicApplierToString(column));
                            builder.append("_");
                            builder.append(slow);
                            builder.append("_");
                            builder.append(fast);
                            builder.append("_");
                            builder.append(prefix);
                            builder.append(",");
                        });
                    });
                });
            });

            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(rocValues).forEach(roc->{
                    builder.append("roc_");
                    builder.append(mapIndicatorLogicApplierToString(column));
                    builder.append("_");
                    builder.append(roc);
                    builder.append(",");
                });
            });

            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(rsiValues).forEach(rsi->{
                    builder.append("rsi_");
                    builder.append(mapIndicatorLogicApplierToString(column));
                    builder.append("_");
                    builder.append(rsi);
                    builder.append(",");
                });
            });

            for(int i=0;i<stochasticKPValues.length;i++){
                for(int j=0;j<stochasticColumns.length;j++){
                    builder.append("sto_");
                    builder.append(stochasticKPValues[i]);
                    builder.append("_");
                    builder.append(stochasticDPValues[i]);
                    builder.append("_");
                    builder.append(stochasticDnPValues[i]);
                    builder.append("_");
                    builder.append(stochasticColumns[j]);
                    if(i == stochasticKPValues.length-1 && j == stochasticColumns.length-1)
                        builder.append("\n");
                    else
                        builder.append(",");
                }
            }
            return builder.toString();

        }

        public String generateRow(LocalDateTime time,Double open,Double close,Double high,Double low){
            builder.delete(0,builder.length());
            builder.append(time);
            builder.append(",");
            builder.append(open*this.ticker.getMultiplier());
            builder.append(",");
            builder.append(close*this.ticker.getMultiplier());
            builder.append(",");
            builder.append(high*this.ticker.getMultiplier());
            builder.append(",");
            builder.append(low*this.ticker.getMultiplier());
            builder.append(",");
            adxIndicators.stream().forEach(adx->{
                builder.append(adx.getADX());
                builder.append(",");
                builder.append(adx.getPlusDI());
                builder.append(",");
                builder.append(adx.getMinusDI());
                builder.append(",");
            });
            crossEMADiffIndicators.stream().forEach(ced->{
                builder.append(ced.getSlowEMAPriceDiffPercent().doubleValue());
                builder.append(",");
                builder.append(ced.getFastEMAPriceDiffPercent().doubleValue());
                builder.append(",");
            });
            rocIndicators.stream().forEach(roc->{
                builder.append(roc.getValue().doubleValue());
                builder.append(",");
            });
            rsiIndicators.stream().forEach(rsi->{
                builder.append(rsi.getValue().doubleValue());
                builder.append(",");
            });
            stochasticIndicators.stream().forEach(sto->{
                builder.append(sto.getKP().doubleValue());
                builder.append(",");
                builder.append(sto.getDP().doubleValue());
                builder.append(",");
                builder.append(sto.getDNP().doubleValue());
                builder.append(",");
            });
            builder.deleteCharAt(builder.length()-1);
            builder.append("\n");
            return builder.toString();
        }

        public void update(LocalDateTime time,Double open,Double close,Double high,Double low){
            CandleData candleData = new CandleData();
            candleData.setTime(time);
            candleData.setOpen(open*this.ticker.getMultiplier());
            candleData.setClose(close*this.ticker.getMultiplier());
            candleData.setHigh(high*this.ticker.getMultiplier());
            candleData.setLow(low*this.ticker.getMultiplier());
            adxIndicators.stream().forEach(adx->adx.update(candleData));
            crossEMADiffIndicators.stream().forEach(ced->ced.update(candleData));
            rocIndicators.stream().forEach(roc->roc.update(candleData));
            rsiIndicators.stream().forEach(rsi->rsi.update(candleData));
            stochasticIndicators.stream().forEach(sto->sto.update(candleData));
        }

        private void createIndicators(){
            Arrays.stream(adxValues).forEach(period->{
                adxIndicators.add(new IndicatorADX(period));
            });
            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(crossEMADiffSlowValues).forEach(slow->{
                    Arrays.stream(crossEMADiffFastValues).forEach(fast->{
                        crossEMADiffIndicators.add(new IndicatorCrossEMADiffPercent(slow,fast,column));
                    });
                });
            });
            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(rocValues).forEach(roc->{
                    rocIndicators.add(new IndicatorROC(roc,column));
                });
            });
            Arrays.stream(valueSelector).forEach(column->{
                Arrays.stream(rsiValues).forEach(rsi->{
                    rsiIndicators.add(new IndicatorRSI(rsi,column));
                });
            });
            for(int i=0;i<stochasticKPValues.length;i++){
                stochasticIndicators.add(new IndicatorStochastic(stochasticKPValues[i],stochasticDPValues[i],stochasticDnPValues[i]));
            }
        }

        private String mapIndicatorLogicApplierToString(IndicatorLogicApplier logicApplier){
            switch (logicApplier){
                case FOR_OPEN: return "open";
                case FOR_HIGH: return "high";
                case FOR_LOW: return "low";
                case FOR_CLOSE: return "close";
            }
            return "close";
        }
    }
}
