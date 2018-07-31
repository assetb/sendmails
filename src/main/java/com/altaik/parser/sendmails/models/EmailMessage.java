package com.altaik.parser.sendmails.models;

import com.altaik.bo.ProcessedPurchase;

import java.util.List;

/**
 * Created by admin on 25.09.2017.
 */
public class EmailMessage{
    private String email;
    private List<ProcessedPurchase> senderPurchase;
}
