package com.distiya.fxscrapper.domain;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OandaTransaction {
    private String id;
    private String accountBalance;
    private String accountID;
    private String batchID;
    private String financing;
    private String instrument;
    private String orderID;
    private String pl;
    private String price;
    private String reason;
    private String time;
    private OandaTradeOpened tradeOpened;
    private String type;
    private String units;
    private String userID;
}
