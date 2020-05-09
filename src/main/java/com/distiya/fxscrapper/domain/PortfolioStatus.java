package com.distiya.fxscrapper.domain;

import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Currency;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.primitives.InstrumentName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PortfolioStatus {
    private double margin = 0;
    private Map<String,TradeInstrument> tradeInstrumentMap;
    private Map<String,TradeInstrument> homeInstrumentMap;
    private AccountID currentAccount;
    private Currency homeCurrency;
    private Account account;
    private CandlestickGranularity tradingGranularity;
    private List<Instrument> tradableInstruments;
    private Set<InstrumentName> allPairs = new HashSet<>();
}
