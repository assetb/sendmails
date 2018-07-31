/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.ets.bo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 */
public class Envelope {
    private int id;
    private String email;
    private String description;
    //    private Customer customer;
    private List<ProcPurchase> procPurchases = new ArrayList<>();
    private boolean sended;

    public Envelope(String email) {
        setEmail(email);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

//    public Envelope(Customer customer) {
//        setCustomer(customer);
//    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

//    public Customer getCustomer() {
//        return customer;
//    }
//
//    public void setCustomer(Customer customer) {
//        this.customer = customer;
//    }

    public boolean isSended() {
        return sended;
    }

    public void setSended(boolean sended) {
        this.sended = sended;
    }

    public List<ProcPurchase> getPurchases() {
        return procPurchases;
    }
}
