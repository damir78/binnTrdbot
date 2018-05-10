/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot;

import com.evgcompany.binntrdbot.misc.NumberFormatter;
import java.math.BigDecimal;

/**
 *
 * @author EVG_Adminer
 */
public class OrderPairItem {
    private String symbolBase = "";
    private String symbolPair = "";
    private String symbolQuote = "";
    private CoinBalanceItem base_item = null;
    private CoinBalanceItem quote_item = null;
    
    private BigDecimal summOrderBase = BigDecimal.ZERO;
    private BigDecimal summOrderQuote = BigDecimal.ZERO;
    
    private boolean in_order_buy_sell_cycle;
    private boolean order_pending;
    private boolean in_sell_order = false;
    private BigDecimal last_order_price = BigDecimal.ZERO;
    private BigDecimal price = BigDecimal.ZERO;
    
    private String marker = "";
    
    private int listIndex = -1;
    
    public OrderPairItem(CoinBalanceItem base_item, CoinBalanceItem quote_item, String symbolPair) {
        this.base_item = base_item;
        this.quote_item = quote_item;
        symbolBase = base_item.getSymbol();
        symbolQuote = quote_item.getSymbol();
        this.symbolPair = symbolPair;
        in_sell_order = false;
        listIndex = -1;
        in_order_buy_sell_cycle = false;
        order_pending = false;
        price = BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        String txt;
        txt = base_item.getSymbol() + ": " + NumberFormatter.df6.format(base_item.getFreeValue());
        if (base_item.getLimitValue().compareTo(BigDecimal.ZERO) > 0) {
            txt = txt + " / " + NumberFormatter.df6.format(base_item.getLimitValue());
        }
        if (base_item.isPairKey()) {
            txt = txt + " ["+symbolPair+"]";
        }
        txt = txt + "  ";
        boolean is_changed = base_item.getValue().compareTo(base_item.getInitialValue()) != 0 || in_order_buy_sell_cycle || order_pending || !marker.isEmpty();
        if (is_changed) {
            
            if (base_item.getInitialValue().compareTo(BigDecimal.ZERO) > 0) {
                txt = txt + "[Init: " + NumberFormatter.df6.format(base_item.getInitialValue());
                float percent = 100 * (base_item.getValue().floatValue() - base_item.getInitialValue().floatValue()) / base_item.getInitialValue().floatValue();
                if (Math.abs(percent) > 0.0005) {
                    txt = txt + " " + (percent >= 0 ? "+" : "") + NumberFormatter.df3.format(percent) + "%";
                }
                txt = txt + "]";
            }
            
            /*if (!base_item.isPairKey()) {
                
            } else {
                txt = txt + "pair";
            }*/
            
            /*if (base_item.getOrdersCount() > 0) {
                txt = txt + "; " + base_item.getOrdersCount() + " orders";
            }*/
            
            if (marker.isEmpty()) {
                if (order_pending) {
                    txt = txt + " [PENDING "+(in_sell_order ? "SELL" : "BUY")+"]";
                } else if (in_order_buy_sell_cycle) {
                    txt = txt + " [ORDER]";
                }
            }
            
            if (last_order_price != null && last_order_price.compareTo(BigDecimal.ZERO) > 0) {
                txt = txt + " [Ord: "+NumberFormatter.df8.format(last_order_price)+ " " + symbolQuote + "]";
            }
        }
 
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            txt = txt + " [Cur: "+NumberFormatter.df8.format(price)+ " " + symbolQuote;
            
            if (last_order_price != null && last_order_price.compareTo(BigDecimal.ZERO) > 0) {
                float percent = 100 * (price.floatValue() - last_order_price.floatValue()) / last_order_price.floatValue();
                if (Math.abs(percent) > 0.0005) {
                    txt = txt + " " + (percent >= 0 ? "+" : "") + NumberFormatter.df3.format(percent) + "%";
                }
            }
            
            txt = txt + "]";
        }
        
        if (!marker.isEmpty()) {
            txt = txt + " ["+marker+"]";
        }
        
        return txt;
    }

    public BigDecimal getValue() {
        return base_item.getValue();
    }
    
    public void setInitialValue(BigDecimal val) {
        base_item.setInitialValue(val);
    }
    
    public void preBuyTransaction(BigDecimal summBase, BigDecimal summQuote, boolean change_initial_values) {
        if (!in_order_buy_sell_cycle) {
            last_order_price = price;
            if (change_initial_values) {
                base_item.addInitialValue(summBase.multiply(new BigDecimal("-1")));
                quote_item.addInitialValue(summQuote);
            }
            in_order_buy_sell_cycle = true;
            order_pending = false;
            in_sell_order = false;
            summOrderBase = BigDecimal.ZERO;
            summOrderQuote = BigDecimal.ZERO;
        }
    }

    public void startBuyTransaction(BigDecimal summBase, BigDecimal summQuote) {
        if (!in_order_buy_sell_cycle) {
            last_order_price = price;
            summOrderBase = summBase;
            summOrderQuote = summQuote;
            quote_item.addLimitValue(summOrderQuote);
            quote_item.addFreeValue(summOrderQuote.multiply(new BigDecimal("-1")));
            base_item.incActiveOrders();
            quote_item.incActiveOrders();
            in_sell_order = false;
            in_order_buy_sell_cycle = true;
            order_pending = true;
        }
    }
    public void startSellTransaction(BigDecimal summBase, BigDecimal summQuote) {
        if (in_order_buy_sell_cycle) {
            summOrderBase = summBase;
            summOrderQuote = summQuote;
            base_item.addLimitValue(summOrderBase);
            base_item.addFreeValue(summOrderBase.multiply(new BigDecimal("-1")));
            base_item.incActiveOrders();
            quote_item.incActiveOrders();
            in_sell_order = true;
            order_pending = true;
        }
    }
    public void confirmTransaction() {
        if (in_sell_order) {
            base_item.addLimitValue(summOrderBase.multiply(new BigDecimal("-1")));
            quote_item.addFreeValue(summOrderQuote);
            in_order_buy_sell_cycle = false;
            last_order_price = BigDecimal.ZERO;
            base_item.incOrderCount();
            quote_item.incOrderCount();
        } else {
            quote_item.addLimitValue(summOrderQuote.multiply(new BigDecimal("-1")));
            base_item.addFreeValue(summOrderBase);
        }
        summOrderBase = BigDecimal.ZERO;
        summOrderQuote = BigDecimal.ZERO;
        in_sell_order = false;
        order_pending = false;
        base_item.decActiveOrders();
        quote_item.decActiveOrders();
    }

    public void confirmTransactionPart(BigDecimal summPartBase, BigDecimal summPartQuote) {
        if (in_sell_order) {
            base_item.addLimitValue(summPartBase.multiply(new BigDecimal("-1")));
            quote_item.addFreeValue(summPartQuote);
        } else {
            quote_item.addLimitValue(summPartQuote.multiply(new BigDecimal("-1")));
            base_item.addFreeValue(summPartBase);
        }
        summOrderBase = summOrderBase.subtract(summPartBase);
        summOrderQuote = summOrderQuote.subtract(summPartQuote);
    }

    public void rollbackTransaction() {
        if (in_sell_order) {
            base_item.addLimitValue(summOrderBase.multiply(new BigDecimal("-1")));
            base_item.addFreeValue(summOrderBase);
        } else {
            quote_item.addLimitValue(summOrderQuote.multiply(new BigDecimal("-1")));
            quote_item.addFreeValue(summOrderQuote);
            in_order_buy_sell_cycle = false;
            last_order_price = BigDecimal.ZERO;
        }
        summOrderBase = BigDecimal.ZERO;
        summOrderQuote = BigDecimal.ZERO;
        in_sell_order = false;
        order_pending = false;
        base_item.decActiveOrders();
        quote_item.decActiveOrders();
    }
    
    public void simpleBuy(BigDecimal summBase, BigDecimal summQuote) {
        last_order_price = price;
        summOrderBase = summBase;
        summOrderQuote = summQuote;
        quote_item.addFreeValue(summOrderQuote.multiply(new BigDecimal("-1")));
        base_item.addFreeValue(summOrderBase);
    }

    public void simpleSell(BigDecimal summBase, BigDecimal summQuote) {
        last_order_price = price;
        summOrderBase = summBase;
        summOrderQuote = summQuote;
        quote_item.addFreeValue(summOrderQuote);
        base_item.addFreeValue(summOrderBase.multiply(new BigDecimal("-1")));
    }
    
    /**
     * @return the base_item
     */
    public CoinBalanceItem getBaseItem() {
        return base_item;
    }

    /**
     * @return the quote_item
     */
    public CoinBalanceItem getQuoteItem() {
        return quote_item;
    }

    /**
     * @return the symbolBase
     */
    public String getSymbolBase() {
        return symbolBase;
    }

    /**
     * @return the symbolPair
     */
    public String getSymbolPair() {
        return symbolPair;
    }

    /**
     * @return the symbolQuote
     */
    public String getSymbolQuote() {
        return symbolQuote;
    }

    /**
     * @return the listIndex
     */
    public int getListIndex() {
        return listIndex;
    }

    /**
     * @param listIndex the listIndex to set
     */
    public void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }
    
    /**
     * @param price the price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    /**
     * @return the price
     */
    BigDecimal getPrice() {
        return price;
    }

    /**
     * @return the last_order_price
     */
    public BigDecimal getLastOrderPrice() {
        return last_order_price;
    }

    /**
     * @param last_order_price the last_order_price to set
     */
    public void setLastOrderPrice(BigDecimal last_order_price) {
        this.last_order_price = last_order_price;
    }

    /**
     * @return the marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * @param marker the marker to set
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }
}