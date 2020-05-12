package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.OandaTransactionGetStream;
import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
@ConditionalOnProperty(prefix = "app.config.broker",name = "streamingMode",havingValue = "true")
public class OandaStreamService implements IStreamService{

    @Autowired
    @Qualifier("streamWebClient")
    private WebClient streamClient;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Flux<OandaTransactionGetStream> getTransactions(){
        return streamClient.get()
                .uri(ub->ub.path("/v3/accounts/{accountId}/transactions/stream").build(appConfigProperties.getBroker().getApiAccount()))
                .accept(MediaType.APPLICATION_STREAM_JSON)
                .retrieve()
                .bodyToFlux(String.class)
                .map(s-> {
                    try {
                        log.info(s);
                        return objectMapper.readValue(s,OandaTransactionGetStream.class);
                    } catch (JsonProcessingException e) {
                        log.error("JsonProcessingException while converting transaction response : {}",e.getMessage());
                        return new OandaTransactionGetStream();
                    }
                });
    }
}
