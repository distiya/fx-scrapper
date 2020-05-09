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
    private String defaultResolution = "M5";
    private String homeCurrency = "SGD";
    private String orderPlacing = "0 0/5 * ? * *";
    private Boolean tradingMode = false;
    private Boolean scrappingMode = false;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    private LocalDate startDate = LocalDate.now();
    private List<SupportedTickerProperties> supportedTickers = new ArrayList<>();
    private List<SupportedResolutionProperties> supportedResolutions = new ArrayList<>();
}
