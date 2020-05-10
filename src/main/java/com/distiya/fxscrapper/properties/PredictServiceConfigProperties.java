package com.distiya.fxscrapper.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PredictServiceConfigProperties {
    private String host = "localhost";
    private Integer port = 50051;
}
