package com.distiya.fxscrapper.util;

import com.distiya.fxscrapper.domain.PortfolioStatus;
import com.distiya.fxscrapper.domain.TradeInstrument;
import com.oanda.v20.instrument.CandlestickData;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.primitives.Instrument;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.distiya.fxscrapper.constant.Constants.API_DATE_FORMAT;
import static com.distiya.fxscrapper.constant.Constants.API_DATE_FORMATTER;

public class AppUtil {
    public static String formatToApiDateTime(LocalDateTime localDateTime){
        return localDateTime.format(DateTimeFormatter.ofPattern(API_DATE_FORMAT)).replace(" ","T");
    }

    public static LocalDateTime convertToLocalDateTime(String dt){
        return LocalDateTime.parse(dt.substring(0,19).replace("T"," "),DateTimeFormatter.ofPattern(API_DATE_FORMAT));
    }

    public static CandlestickGranularity getCandlestickGranularity(String res){
        switch (res){
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
        return ti;
    }

    public static String[] getBaseHomePair(String instrument,String homeCurrency){
        String[] instrumentSplit = instrument.split("_");
        String firstPair = instrumentSplit[0] + "_" + homeCurrency;
        String secondPair =  homeCurrency + "_" + instrumentSplit[0];
        return new String[]{firstPair,secondPair};
    }

    public static double getUnitCount(String instrument, PortfolioStatus portfolioStatus){
        String[] baseHomePair = getBaseHomePair(instrument, portfolioStatus.getHomeCurrency().toString());
        double baseHomeRate = 1.0;
        double units = 0;
        if(portfolioStatus.getHomeInstrumentMap().containsKey(baseHomePair[0])){
            baseHomeRate = portfolioStatus.getHomeInstrumentMap().get(baseHomePair[0]).getCurrentMarket().getC().doubleValue();
        }
        else if(portfolioStatus.getHomeInstrumentMap().containsKey(baseHomePair[0])){
            baseHomeRate = 1.0/portfolioStatus.getHomeInstrumentMap().get(baseHomePair[0]).getCurrentMarket().getC().doubleValue();
        }
        if(portfolioStatus.getTradeInstrumentMap().containsKey(instrument)){
            TradeInstrument tradeInstrument = portfolioStatus.getTradeInstrumentMap().get(instrument);
            units = portfolioStatus.getMargin() * tradeInstrument.getCurrentFraction()/baseHomeRate;
            tradeInstrument.setMaxUnits(units);
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
}
