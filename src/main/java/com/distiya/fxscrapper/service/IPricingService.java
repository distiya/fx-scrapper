package com.distiya.fxscrapper.service;

import com.oanda.v20.account.AccountID;
import com.oanda.v20.pricing.PricingGetResponse;

import java.util.Optional;
import java.util.Set;

public interface IPricingService {
    Optional<PricingGetResponse> getLatestPriceForInstruments(AccountID accountID, Set<String> instruments);
    Optional<PricingGetResponse> getLatestPriceForInstrumentsWithCurrentAccount(Set<String> instruments);
}
