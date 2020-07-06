package com.distiya.fxscrapper.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class BrokerConfigProperties {
    private String name = "";
    private String mode = "";
    private String apiToken = "";
    private String apiAccount = "";
    private String apiUrl = "";
    private String apiStreamUrl = "";
    private String dataLocation = "";
    private String timeZone = "America/New_York";
    private Long recordCount = 5000l;
    private String candleMode = "M";
    private String lowTimeFrame = "M5";
    private String highTimeFrame = "M10";
    private Long defaultPredictBatchLength = 48l;
    private String homeCurrency = "SGD";
    private String orderPlacing = "0 0/5 * ? * *";
    private String highScreenScheduling = "0 0/5 * ? * *";
    private String dailyPreparation = "0 0/5 * ? * *";
    private Boolean tradingMode = false;
    private Boolean scrappingMode = false;
    private Boolean streamingMode = false;
    private Boolean streamingSubscriber = false;
    private Boolean skipFutureTrades = false;
    private Double leftMargin = 0.0;
    private Double minCandleDiff = 0.002;
    private Double marginRatio = 0.05;
    private Double minTradeProfitPercentage = 0.1;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate startDate = LocalDate.now();
    private List<SupportedTickerProperties> supportedTickers = new ArrayList<>();
    private List<SupportedResolutionProperties> supportedResolutions = new ArrayList<>();
}
