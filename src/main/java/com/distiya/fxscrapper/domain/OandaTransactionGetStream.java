package com.distiya.fxscrapper.domain;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class OandaTransactionGetStream {
    private String type;
    private String time;
    private OandaTransaction transaction;
    private String lastTransactionID;
}
