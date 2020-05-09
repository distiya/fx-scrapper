package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.domain.OandaTransactionGetStream;
import reactor.core.publisher.Flux;

public interface IStreamService {
    Flux<OandaTransactionGetStream> getTransactions();
}
