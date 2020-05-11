package com.distiya.fxscrapper.constant;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class Constants {
    public static final String API_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern(API_DATE_FORMAT);
    public static DecimalFormat priceFormat = new DecimalFormat("0.00000");
}
