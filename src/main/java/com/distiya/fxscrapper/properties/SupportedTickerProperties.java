package com.distiya.fxscrapper.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupportedTickerProperties {
    private String ticker = "";
    private Boolean enabled = false;
    private Boolean isIndex = false;
    private String baseCurrency = "USD";
    private Double multiplier = 1.0d;
}
