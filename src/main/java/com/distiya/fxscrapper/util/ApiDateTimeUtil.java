package com.distiya.fxscrapper.util;

import com.distiya.fxscrapper.constant.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ApiDateTimeUtil {
    public static String formatToApiDateTime(LocalDateTime localDateTime){
        return localDateTime.format(DateTimeFormatter.ofPattern(Constants.API_DATE_FORMAT)).replace(" ","T");
    }

    public static LocalDateTime convertToLocalDateTime(String dt){
        return LocalDateTime.parse(dt.substring(0,19).replace("T"," "),DateTimeFormatter.ofPattern(Constants.API_DATE_FORMAT));
    }
}
