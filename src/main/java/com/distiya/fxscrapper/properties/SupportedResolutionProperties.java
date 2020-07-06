package com.distiya.fxscrapper.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupportedResolutionProperties {
    private String symbol = "";
    private Integer seconds = 0;
    private Integer minutes = 0;
    private Integer hours = 0;
    private Integer days = 0;
    private Integer weeks = 0;
    private Integer months = 0;
    private Boolean enabled = false;
}
