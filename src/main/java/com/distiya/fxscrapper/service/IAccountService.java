package com.distiya.fxscrapper.service;

import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.primitives.Instrument;

import java.util.List;
import java.util.Optional;

public interface IAccountService {
    Optional<Account> getAccount(AccountID accountID);
    Optional<AccountSummary> getAccountSummary(AccountID accountID);
    Optional<List<Instrument>> getTradableInstruments(AccountID accountID);
    Optional<Account> getCurrentAccount();
    Optional<AccountSummary> getCurrentAccountSummary();
    Optional<List<Instrument>> getTradableInstrumentsForCurrentAccount();
}
