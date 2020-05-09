package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.request.CandleHistoryRequest;
import com.distiya.fxscrapper.util.AppUtil;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.primitives.InstrumentName;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class OandaHistoryService implements IHistoryService{

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private Context context;

    @Override
    public void requestHistory(String ticker, SupportedResolutionProperties resolution, LocalDate startDate){
        CandleHistoryRequest candleHistoryRequest = new CandleHistoryRequest(appConfigProperties,ticker,resolution,startDate);
        Path path = Paths.get(candleHistoryRequest.getDataFileName());
        File dataFile = path.toFile();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(dataFile))){
            bw.write("time,open,close,high,low,volume\n");
            log.info("Data file writing started for "+candleHistoryRequest.getDataFileName());
            requestHistoryForRound(candleHistoryRequest,bw);
            log.info("Data file writing ended for "+candleHistoryRequest.getDataFileName());
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
        request.setDailyAlignment(0l);
        request.setAlignmentTimezone(appConfigProperties.getBroker().getTimeZone());
        try {
            return Optional.of(context.instrument.candles(request).getCandles());
        } catch (RequestException e) {
            log.error("RequestException while requesting candles for instrument {} with granularity {} and count {} : {}",instrument.toString(),granularity.toString(),count,e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while requesting candles for instrument {} with granularity {} and count {} : {}",instrument.toString(),granularity.toString(),count,e.getMessage());
        }
        return Optional.empty();
    }

    private void requestHistoryForRound(CandleHistoryRequest candleHistoryRequest,BufferedWriter bw) {
        try {
            InstrumentCandlesResponse candleResponse = context.instrument.candles(candleHistoryRequest.getRequest());
            if(!candleResponse.getCandles().isEmpty()){
                candleResponse.getCandles().stream().forEach(c->{
                    String record = c.getTime().toString() + "," + c.getMid().getO() + "," +c.getMid().getC() + ","  +c.getMid().getH() + "," +c.getMid().getL() + "," + c.getVolume() + "\n";
                    try {
                        bw.write(record);
                    } catch (IOException e) {
                        log.error("IOException in writing data record {}",e.getMessage());
                    }
                });
                candleHistoryRequest.updateFromTime(candleHistoryRequest.getOrs().getIncrement().apply(AppUtil.convertToLocalDateTime(candleResponse.getCandles().get(candleResponse.getCandles().size()-1).getTime().toString())));
                requestHistoryForRound(candleHistoryRequest,bw);
            }
        } catch (RequestException e) {
            log.error("RequestException {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException {}",e.getMessage());
        }
    }
}
