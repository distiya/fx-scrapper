package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.request.CandleHistoryRequest;
import com.distiya.fxscrapper.util.ApiDateTimeUtil;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
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

@Service
@Slf4j
public class OandaHistoryService {

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private Context context;

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
                candleHistoryRequest.updateFromTime(candleHistoryRequest.getOrs().getIncrement().apply(ApiDateTimeUtil.convertToLocalDateTime(candleResponse.getCandles().get(candleResponse.getCandles().size()-1).getTime().toString())));
                requestHistoryForRound(candleHistoryRequest,bw);
            }
        } catch (RequestException e) {
            log.error("RequestException {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException {}",e.getMessage());
        }
    }
}
