/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot.coinrating;

/**
 *
 * @author EVG_adm_T
 */
@FunctionalInterface
public interface AccountCostUpdateEvent {
    void onUpdate(CoinInfoAggregator agg);
}