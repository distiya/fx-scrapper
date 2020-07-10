package com.distiya.fxscrapper.util;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Instrument;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.distiya.fxscrapper.constant.Constants.*;

public class AppUtil {
    public static String formatToApiDateTime(LocalDateTime localDateTime){
        return localDateTime.format(DateTimeFormatter.ofPattern(API_DATE_FORMAT)).replace(" ","T");
    }

    public static LocalDateTime convertToLocalDateTime(String dt){
        return LocalDateTime.parse(dt.substring(0,19).replace("T"," "),DateTimeFormatter.ofPattern(API_DATE_FORMAT));
    }

    public static CandlestickGranularity getCandlestickGranularity(String res){
        switch (res){
            case "S5":return CandlestickGranularity.S5;
            case "S10":return CandlestickGranularity.S10;
            case "S15":return CandlestickGranularity.S15;
            case "S30":return CandlestickGranularity.S30;
            case "M1":return CandlestickGranularity.M1;
            case "M2":return CandlestickGranularity.M2;
            case "M10":return CandlestickGranularity.M10;
            case "M15":return CandlestickGranularity.M15;
            case "M30":return CandlestickGranularity.M30;
            case "H1":return CandlestickGranularity.H1;
            case "H2":return CandlestickGranularity.H2;
            case "H3":return CandlestickGranularity.H3;
            case "H4":return CandlestickGranularity.H4;
            case "H6":return CandlestickGranularity.H6;
            case "H8":return CandlestickGranularity.H8;
            case "H12":return CandlestickGranularity.H12;
            case "D":return CandlestickGranularity.D;
            case "W":return CandlestickGranularity.W;
            case "M":return CandlestickGranularity.M;
            case "M5":
            default:return CandlestickGranularity.M5;
        }
    }

    public static TradeInstrument mapToTradeInstrument(Instrument instrument){
        TradeInstrument ti = new TradeInstrument();
        ti.setTicker(instrument.getName().toString());
        ti.setInstrument(instrument);
        ti.updateIndicators();
        return ti;
    }

    public static String[] getBaseHomePair(String instrument,String homeCurrency){
        String[] instrumentSplit = instrument.split("_");
        String firstPair = instrumentSplit[0] + "_" + homeCurrency;
        String secondPair =  homeCurrency + "_" + instrumentSplit[0];
        return new String[]{firstPair,secondPair};
    }

    public static double getUnitCount(String instrument, PortfolioStatus portfolioStatus){
        double baseHomeRate = 1.0;
        double units = 0;
        String[] baseHomePair;
        if(portfolioStatus.getIndexTickers().containsKey(instrument)){
            baseHomePair = new String[2];
            baseHomePair[0] = portfolioStatus.getIndexTickers().get(instrument).stream().findFirst().map(si->si.getBaseCurrency()).orElse("USD") + "_" + portfolioStatus.getHomeCurrency().toString();
            baseHomePair[1] = portfolioStatus.getHomeCurrency().toString() + "_" + portfolioStatus.getIndexTickers().get(instrument).stream().findFirst().map(si->si.getBaseCurrency()).orElse("USD");
        }
        else{
            baseHomePair = getBaseHomePair(instrument, portfolioStatus.getHomeCurrency().toString());
        }
        if(portfolioStatus.getHomeInstrumentMap().containsKey(baseHomePair[0])){
            baseHomeRate = portfolioStatus.getHomeInstrumentMap().get(baseHomePair[0]).getCurrentLowMarket().getC().doubleValue();
        }
        else if(portfolioStatus.getHomeInstrumentMap().containsKey(baseHomePair[1])){
            baseHomeRate = 1.0/portfolioStatus.getHomeInstrumentMap().get(baseHomePair[1]).getCurrentLowMarket().getC().doubleValue();
        }
        if(portfolioStatus.getTradeInstrumentMap().containsKey(instrument)){
            TradeInstrument tradeInstrument = portfolioStatus.getTradeInstrumentMap().get(instrument);
            units = (portfolioStatus.getMargin()/portfolioStatus.getAppConfigProperties().getBroker().getMarginRatio()) * tradeInstrument.getCurrentFraction()/baseHomeRate;
            if(portfolioStatus.getIndexTickers().containsKey(instrument)){
                units = units/tradeInstrument.getCurrentLowMarket().getC().doubleValue();
                if(units >= 3.0){
                    units = units - 2.0;
                }
            }
            tradeInstrument.setMaxUnits(units > 0 ? units : 0.0d);
        }
        return units;
    }

    public static double calculateHighPrice(CandlestickData currentMarket, CandlestickData currentPredict, CandlestickData previousPredict){
        return currentMarket.getH().doubleValue() + currentPredict.getH().doubleValue() - previousPredict.getH().doubleValue();
    }

    public static double calculateLowPrice(CandlestickData currentMarket,CandlestickData currentPredict,CandlestickData previousPredict){
        return currentMarket.getL().doubleValue() + currentPredict.getL().doubleValue() - previousPredict.getL().doubleValue();
    }

    public static String getCurrentTime(){
        return LocalDateTime.now().format(API_DATE_FORMATTER);
    }

    public static Double formatPrice(Double inputPrice){
        return Double.valueOf(priceFormat.format(inputPrice));
    }

    public static void updateAllCurrentFraction(List<TradeInstrument> tradableInstruments){
        double totalFraction = tradableInstruments.stream().mapToDouble(ti->ti.getDailyVolume()).sum();
        tradableInstruments.stream().forEach(ti->{
            ti.setCurrentFraction(totalFraction > 0 ? ti.getDailyVolume()/totalFraction : 0.0);
        });
    }

    public static void updateAllMaxUnitCount(List<TradeInstrument> tradableInstruments,PortfolioStatus portfolioStatus){
        tradableInstruments.stream().forEach(ti->{
            AppUtil.getUnitCount(ti.getInstrument().getName().toString(),portfolioStatus);
        });
    }
}
