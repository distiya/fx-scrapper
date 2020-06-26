package com.distiya.fxscrapper.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminConfig {
    private Double takeProfitPercentage;
    private Boolean skipFutureTrades;
    private Double leftMarginAmount;
}
