package com.distiya.fxscrapper.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "app.config")
public class AppConfigProperties {
    private BrokerConfigProperties broker = new BrokerConfigProperties();
}
