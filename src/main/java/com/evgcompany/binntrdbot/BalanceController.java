/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot;

import com.binance.api.client.domain.account.AssetBalance;
import com.evgcompany.binntrdbot.api.TradingAPIAbstractInterface;
import com.evgcompany.binntrdbot.events.AccountCostUpdateEvent;
import com.evgcompany.binntrdbot.coinrating.CoinInfoAggregator;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import javax.swing.DefaultListModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author EVG_Adminer
 */
public class BalanceController extends PeriodicProcessThread {
    
    private static final Semaphore SEMAPHORE = new Semaphore(1, true);
    private static final BalanceController instance = new BalanceController();
    private CoinInfoAggregator info = null;
    private TradingAPIAbstractInterface client = null;
    private final Map<String, CoinBalanceItem> coins = new HashMap<>();
    private final DefaultListModel<String> listProfitModel = new DefaultListModel<>();

    private boolean isTestMode = false;
    private Closeable socket = null;
    
    private double baseAccountCost = 0;
    private double initialAccountCost = -1;
    private AccountCostUpdateEvent accountCostUpdate = null;

    private BalanceController() {
        info = CoinInfoAggregator.getInstance();
    }
    
    public static BalanceController getInstance(){
        return instance;
    }
    
    private double getAccountCost(String coin) {
        double accountCost = 0;
        for (Map.Entry<String, CoinBalanceItem> entry : coins.entrySet()) {
            accountCost += info.convertSumm(entry.getValue().getSymbol(), entry.getValue().getValue().doubleValue(), coin);
        }
        return accountCost;
    }
    
    public void updateBaseAccountCost() {
        if (coins.isEmpty()) return;
        double tmpCost = baseAccountCost;
        baseAccountCost = getAccountCost(info.getBaseCoin());
        if (initialAccountCost < 0) {
            initialAccountCost = baseAccountCost;
        }
        if (accountCostUpdate != null && tmpCost != baseAccountCost) {
            accountCostUpdate.onUpdate(this);
        }
    }
    
    public CoinBalanceItem getCoinBalanceInfo(String symbolAsset) {
        return coins.get(symbolAsset);
    }
    
    private void showTradeComissionCurrency() {
        if (client != null && client.getTradeComissionCurrency() != null) {
            setCoinVisible(client.getTradeComissionCurrency());
        }
    }
    
    public BigDecimal getOrderAssetAmount(String symbolAsset, BigDecimal percent) {
        CoinBalanceItem quote = coins.get(symbolAsset);
        if (quote != null) {
            BigDecimal quoteBalance;
            if (quote.getInitialValue().compareTo(quote.getValue()) >= 0) {
                quoteBalance = quote.getInitialValue();
            } else {
                quoteBalance = quote.getValue();
            }
            quoteBalance = quoteBalance.multiply(percent.divide(BigDecimal.valueOf(100)));
            if (quote.getFreeValue().compareTo(quoteBalance) < 0) {
                quoteBalance = quote.getFreeValue();
            }
            return quoteBalance;
        }
        return BigDecimal.ZERO;
    }
    
    public boolean canBuy(String symbolPair, BigDecimal baseAmount, BigDecimal price) {
        String symbolQuote = info.getPairQuoteSymbol(symbolPair);
        return (symbolQuote != null) && coins.containsKey(symbolQuote) && baseAmount.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(BigDecimal.ZERO) > 0 && (coins.get(symbolQuote).getFreeValue().compareTo(baseAmount.multiply(price)) >= 0);
    }
    public boolean canSell(String symbolPair, BigDecimal baseAmount) {
        String symbolBase = info.getPairBaseSymbol(symbolPair);
        return (symbolBase != null) && coins.containsKey(symbolBase) && baseAmount.compareTo(BigDecimal.ZERO) > 0 && (coins.get(symbolBase).getFreeValue().compareTo(baseAmount) >= 0);
    }
    public BigDecimal getAvailableCount(String assetSymbol) {
        CoinBalanceItem citem = coins.get(assetSymbol);
        if (citem == null) return null;
        return citem.getFreeValue();
    }
    
    public void testPayTradeComission(BigDecimal price, String quoteSymbol) {
        boolean use_spec_currency = client.getTradeComissionCurrency() != null && !client.getTradeComissionCurrency().equals(quoteSymbol);
        BigDecimal comission_quote;
        if (use_spec_currency) {
            comission_quote = price.multiply(client.getTradeComissionCurrencyPercent()).divide(BigDecimal.valueOf(100));
            String comission_pair = (client.getTradeComissionCurrency() + quoteSymbol).toUpperCase();
            BigDecimal pair_price = null;
            if (info.getLastPrices().containsKey(comission_pair)) {
                pair_price = BigDecimal.valueOf(info.getLastPrices().get(comission_pair));
            } else {
                pair_price = client.getCurrentPrice(comission_pair);
            }
            if (pair_price != null && pair_price.compareTo(BigDecimal.ZERO) > 0) {
                CoinBalanceItem ccomm = coins.get(client.getTradeComissionCurrency());
                BigDecimal trade_com = comission_quote.divide(pair_price, RoundingMode.HALF_DOWN);
                if (ccomm != null && ccomm.getFreeValue().compareTo(trade_com) > 0) {
                    ccomm.addFreeValue(trade_com.multiply(BigDecimal.valueOf(-1)));
                    updateCoinText(client.getTradeComissionCurrency());
                } else {
                    use_spec_currency = false;
                }
            } else {
                use_spec_currency = false;
            }
        }
        if (!use_spec_currency) {
            comission_quote = price.multiply(client.getTradeComissionPercent()).divide(BigDecimal.valueOf(100));
            CoinBalanceItem cquote = coins.get(quoteSymbol);
            cquote.addFreeValue(comission_quote.multiply(BigDecimal.valueOf(-1)));
        }
    }

    private void updateCoinText(CoinBalanceItem coinItem) {
        if (coinItem != null && coinItem.getListIndex() >= 0) {
            listProfitModel.set(coinItem.getListIndex(), coinItem.toString());
        }
    }
    
    public void updateCoinText(String symbol) {
        updateCoinText(coins.get(symbol));
    }
    
    public void updateAllCoinsTexts() {
        coins.entrySet().forEach((entry) -> {
            updateCoinText(entry.getValue());
        });
    }
    
    public void setCoinVisible(String symbol) {
        CoinBalanceItem curr = coins.get(symbol);
        if (curr != null) {
            if (curr.getListIndex() < 0) {
                curr.setListIndex(listProfitModel.size());
                listProfitModel.addElement(curr.toString());
            } else {
                updateCoinText(curr);
            }
        }
    }
    
    private void updateBalances(List<AssetBalance> allBalances) {
        try {
            SEMAPHORE.acquire();
            for (int i = 0; i < allBalances.size(); i++) {
                String symbol = allBalances.get(i).getAsset().toUpperCase();
                if (info.getCoins().contains(symbol)) {
                    CoinBalanceItem curr;
                    if (!coins.containsKey(symbol)) {
                        curr = new CoinBalanceItem(symbol);
                        curr.setCoinEvent((coinItem)->{
                            updateCoinText(coinItem);
                        });
                    } else {
                        curr = coins.get(symbol);
                    }
                    if (!isTestMode || curr.getInitialValue().compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal balance_free = new BigDecimal(allBalances.get(i).getFree());
                        BigDecimal balance_limit = new BigDecimal(allBalances.get(i).getLocked());
                        curr.setFreeValue(balance_free);
                        curr.setLimitValue(balance_limit);
                        if (curr.getInitialValue().compareTo(BigDecimal.ZERO) < 0) {
                            curr.setInitialValue(curr.getValue());
                        }
                    }
                    if (!coins.containsKey(symbol)) {
                        coins.put(symbol, curr);
                    }
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(BalanceController.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateBaseAccountCost();
        SEMAPHORE.release();
    }
    
    public void updateAllBalances() {
        if (client != null) {
            List<AssetBalance> allBalances = client.getAllBalances();
            updateBalances(allBalances);
        }
    }
    
    @Override
    protected void runStart() {
        info.StartAndWaitForInit();
        updateAllBalances();
        showTradeComissionCurrency();
        if (!isTestMode) {
            socket = client.OnBalanceEvent((balances) -> {
                System.out.println("Balance changed: " + balances);
                updateBalances(balances);
            });
        }
    }

    @Override
    protected void runBody() {
        if (!isTestMode) {
            updateAllBalances();
        }
    }

    @Override
    protected void runFinish() {
        mainApplication.getInstance().log("Stopping balance update thread...");
        if (socket != null) try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(BalanceController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setClient(TradingAPIAbstractInterface _client) {
        client = _client;
    }

    public TradingAPIAbstractInterface getClient() {
        return client;
    }
    
    /**
     * @return the isTestMode
     */
    public boolean isTestMode() {
        return isTestMode;
    }

    /**
     * @param isTestMode the isTestMode to set
     */
    public void setTestMode(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }
    
    /**
     * @return the listProfitModel
     */
    public DefaultListModel<String> getListProfitModel() {
        return listProfitModel;
    }
    
    /**
     * @return the baseAccountCost
     */
    public double getBaseAccountCost() {
        return baseAccountCost;
    }
    
    /**
     * @return the accountCostUpdate
     */
    public AccountCostUpdateEvent getAccountCostUpdate() {
        return accountCostUpdate;
    }

    /**
     * @param accountCostUpdate the accountCostUpdate to set
     */
    public void setAccountCostUpdate(AccountCostUpdateEvent accountCostUpdate) {
        this.accountCostUpdate = accountCostUpdate;
    }

    /**
     * @return the initialAccountCost
     */
    public double getInitialAccountCost() {
        return initialAccountCost;
    }

    public void resetAccountCost() {
        baseAccountCost = 0;
        initialAccountCost = -1;
    }
}
