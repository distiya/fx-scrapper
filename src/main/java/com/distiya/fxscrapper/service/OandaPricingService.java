package com.distiya.fxscrapper.service;

import com.distiya.fxscrapper.properties.AppConfigProperties;
import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
public class OandaPricingService implements IPricingService{

    @Autowired
    private Context context;

    @Autowired
    private AppConfigProperties appConfigProperties;

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Override
    public Optional<PricingGetResponse> getLatestPriceForInstruments(AccountID accountID, Set<String> instruments){
        PricingGetRequest pgr = new PricingGetRequest(accountID,instruments);
        try {
            return Optional.of(context.pricing.get(pgr));
        } catch (RequestException e) {
            log.error("RequestException while getting prices");
        } catch (ExecuteException e) {
            log.error("ExecuteException while getting prices");
        }
        return Optional.empty();
    }

    @Override
    public Optional<PricingGetResponse> getLatestPriceForInstrumentsWithCurrentAccount(Set<String> instruments){
        return getLatestPriceForInstruments(currentAccount,instruments);
    }
}
