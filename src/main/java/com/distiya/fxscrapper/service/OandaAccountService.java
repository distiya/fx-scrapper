package com.distiya.fxscrapper.service;

import com.oanda.v20.Context;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.primitives.Instrument;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@DependsOn("oandaContext")
public class OandaAccountService implements IAccountService {

    @Autowired
    private Context context;

    @Autowired
    @Qualifier("currentAccount")
    private AccountID currentAccount;

    @Override
    public Optional<Account> getAccount(AccountID accountID) {
        try {
            return Optional.of(context.account.get(accountID).getAccount());
        } catch (RequestException e) {
           log.error("RequestException while getting account : {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while getting account : {}",e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<AccountSummary> getAccountSummary(AccountID accountID) {
        try {
            return Optional.of(context.account.summary(accountID).getAccount());
        } catch (RequestException e) {
            log.error("RequestException while getting account summary : {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while getting account summary : {}",e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<Instrument>> getTradableInstruments(AccountID accountID) {
        try {
            return Optional.of(context.account.instruments(accountID).getInstruments());
        } catch (RequestException e) {
            log.error("RequestException while getting tradable instruments : {}",e.getErrorMessage());
        } catch (ExecuteException e) {
            log.error("ExecuteException while getting tradable instruments : {}",e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> getCurrentAccount() {
        return getAccount(currentAccount);
    }

    @Override
    public Optional<AccountSummary> getCurrentAccountSummary() {
        return getAccountSummary(currentAccount);
    }

    @Override
    public Optional<List<Instrument>> getTradableInstrumentsForCurrentAccount() {
        return getTradableInstruments(currentAccount);
    }
}
