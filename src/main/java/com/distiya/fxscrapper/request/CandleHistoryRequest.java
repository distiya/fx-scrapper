package com.distiya.fxscrapper.request;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.distiya.fxscrapper.properties.OandaResolution;
import com.distiya.fxscrapper.properties.SupportedResolutionProperties;
import com.distiya.fxscrapper.util.ApiDateTimeUtil;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.primitives.DateTime;
import com.oanda.v20.primitives.InstrumentName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CandleHistoryRequest {

    private OandaResolution ors;
    private InstrumentCandlesRequest request;
    private String dataFileName;

    public CandleHistoryRequest(AppConfigProperties appConfigProperties,String ticker, SupportedResolutionProperties resolution, LocalDate startDate){
        LocalDateTime initialStart = LocalDateTime.of(startDate, LocalTime.of(0,0));
        this.ors = new OandaResolution(resolution);
        this.request = new InstrumentCandlesRequest(new InstrumentName(ticker));
        this.request.setCount(appConfigProperties.getBroker().getRecordCount());
        this.request.setGranularity(ors.getGranularity());
        this.request.setPrice(appConfigProperties.getBroker().getCandleMode());
        this.request.setDailyAlignment(0l);
        this.request.setIncludeFirst(true);
        this.request.setAlignmentTimezone(appConfigProperties.getBroker().getTimeZone());
        this.dataFileName = getDataFileName(appConfigProperties.getBroker().getDataLocation(),ticker,resolution.getSymbol());
        updateFromTime(initialStart);
    }

    public void updateFromTime(LocalDateTime fromTime){
        this.request.setFrom(new DateTime(ApiDateTimeUtil.formatToApiDateTime(fromTime)));
    }

    private String getDataFileName(String path,String ticker,String res){
        return (path+ticker.replace("_","")+"_"+res+".csv").toLowerCase();
    }
}
