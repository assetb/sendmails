/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.ets.bo;

import java.util.ArrayList;

/**
 * @author admin
 */
public class Envelopes extends ArrayList<Envelope> {
    public Envelope Find(String email) {
        for (Envelope envelope : this) {

            if (envelope.getEmail().equals(email)) {
                return envelope;
            }
        }
        return null;
    }
}
