/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot;

import com.evgcompany.binntrdbot.api.TradingAPIAbstractInterface;
import com.evgcompany.binntrdbot.api.TradingAPIBinance;
import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author EVG_adm_T
 */
public class mainApplication extends javax.swing.JFrame {

    private TradingAPIAbstractInterface client = null;
    
    private static final Semaphore SEMAPHORE_LOG = new Semaphore(1, true);
    
    private List<tradePairProcess> pairs = new ArrayList<>(0);
    private tradeProfitsController profitsChecker = new tradeProfitsController(this);
    private CoinRatingController coinRatingController = new CoinRatingController(this);
    
    private boolean is_paused = false;
    Preferences prefs = null;
    
    private void componentPrefLoad(JComponent cb, String name) {
        if (cb instanceof JCheckBox) {
            ((JCheckBox)cb).setSelected("true".equals(prefs.get(name, ((JCheckBox)cb).isSelected() ? "true" : "false")));
        } else if (cb instanceof JTextField) {
            ((JTextField)cb).setText(prefs.get(name, ((JTextField)cb).getText()));
        } else if (cb instanceof JSpinner) {
            ((JSpinner)cb).setValue(Integer.parseInt(prefs.get(name, ((Integer) ((JSpinner)cb).getValue()).toString())));
        } else if (cb instanceof JComboBox) {
            ((JComboBox<String>)cb).setSelectedIndex(Integer.parseInt(prefs.get(name, ((Integer) ((JComboBox<String>)cb).getSelectedIndex()).toString())));
        }
    }
    private void componentPrefSave(JComponent cb, String name) {
        if (cb instanceof JCheckBox) {
            prefs.put(name, ((JCheckBox)cb).isSelected() ? "true" : "false");
        } else if (cb instanceof JTextField) {
            prefs.put(name, ((JTextField)cb).getText());
        } else if (cb instanceof JSpinner) {
            prefs.put(name, ((JSpinner)cb).getValue().toString());
        } else if (cb instanceof JComboBox) {
            prefs.put(name, ((Integer) ((JComboBox<String>)cb).getSelectedIndex()).toString());
        }
    }
    
    /**
     * Creates new form mainApplication
     */
    public mainApplication() {
        initComponents();
        DefaultCaret caret = (DefaultCaret)logTextarea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        listProfit.setModel(profitsChecker.getListProfitModel());
        listCurrencies.setModel(profitsChecker.getListCurrenciesModel());
        listHeroes.setModel(coinRatingController.getListHeroesModel());
        
        StrategiesController scontroller = new StrategiesController();
        List<String> s_items = new ArrayList<>(scontroller.getStrategiesInitializerMap().keySet());
        java.util.Collections.sort(s_items);
        s_items.forEach((strategy_name)->{
            ComboBoxMainStrategy.addItem(strategy_name);
        });
        
        prefs = Preferences.userNodeForPackage(this.getClass());
        setBounds(
            Integer.parseInt(prefs.get("window_pos_x", String.valueOf(Math.round(getBounds().getX())))),
            Integer.parseInt(prefs.get("window_pos_y", String.valueOf(Math.round(getBounds().getY())))),
            Integer.parseInt(prefs.get("window_size_x", String.valueOf(Math.round(getBounds().getWidth())))),
            Integer.parseInt(prefs.get("window_size_y", String.valueOf(Math.round(getBounds().getHeight()))))
        );
        componentPrefLoad(textFieldApiKey, "api_key");
        componentPrefLoad(textFieldApiSecret, "api_secret");
        componentPrefLoad(textFieldTradePairs, "trade_pairs");
        componentPrefLoad(checkboxTestMode, "test_mode");
        componentPrefLoad(checkBoxLowHold, "low_hold");
        componentPrefLoad(checkboxAutoFastorder, "auto_heroes");
        componentPrefLoad(checkBoxAutoAnalyzer, "auto_anal");
        componentPrefLoad(checkBoxLimitedOrders, "limited_orders");
        componentPrefLoad(checkBoxCheckOtherStrategies, "strategies_add_check");
        componentPrefLoad(spinnerUpdateDelay, "update_delay");
        componentPrefLoad(spinnerBuyPercent, "buy_percent");
        componentPrefLoad(comboBoxBarsInterval, "bars");
        componentPrefLoad(comboBoxBarsCount, "bars_queries_index");
        componentPrefLoad(ComboBoxMainStrategy, "main_strategy");
        componentPrefLoad(checkBoxStopGain, "use_stop_gain");
        componentPrefLoad(checkBoxStopLoss, "use_stop_loss");
        componentPrefLoad(spinnerStopGain, "stop_gain");
        componentPrefLoad(spinnerStopLoss, "stop_loss");
        componentPrefLoad(checkBoxBuyStopLimited, "use_buy_stop_limited_timeout");
        componentPrefLoad(spinnerBuyStopLimited, "buy_stop_limited_timeout");
        componentPrefLoad(checkBoxSellStopLimited, "use_sell_stop_limited_timeout");
        componentPrefLoad(spinnerSellStopLimited, "sell_stop_limited_timeout");
        componentPrefLoad(comboBoxLimitedMode, "limited_mode");
    }

    private int searchCurrencyFirstPair(String currencyPair, boolean is_hodling) {
        for(int i=0; i<pairs.size(); i++) {
            if (pairs.get(i).getSymbol().equals(currencyPair) && pairs.get(i).isHodling() == is_hodling) {
                return i;
            }
        }
        return -1;
    }
    private int searchCurrencyFirstPair(String currencyPair) {
        for(int i=0; i<pairs.size(); i++) {
            if (pairs.get(i).getSymbol().equals(currencyPair)) {
                return i;
            }
        }
        return -1;
    }
    
    public void log(String txt) {
        try {
            SEMAPHORE_LOG.acquire();
            logTextarea.append(txt);
            logTextarea.append("\n");
            SEMAPHORE_LOG.release();
        } catch (InterruptedException e) {}
    }
    public void log(String txt, boolean is_main, boolean with_date) {
        try {
            SEMAPHORE_LOG.acquire();
            if (with_date) {
                Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                txt = formatter.format(Calendar.getInstance().getTime()) + ": " + txt;
            }
            logTextarea.append(txt);
            logTextarea.append("\n");
            if (is_main) {
                mainTextarea.append(txt);
                mainTextarea.append("\n");
            }
            SEMAPHORE_LOG.release();
        } catch (InterruptedException e) {}
    }
    public void log(String txt, boolean is_main) {
        log(txt, is_main, false);
    }
    
    public List<tradePairProcess> getPairs() {
        return pairs;
    }
    public tradeProfitsController getProfitsChecker() {
        return profitsChecker;
    }
    
    private void initAPI() {
        if (client == null) {
            client = new TradingAPIBinance(textFieldApiSecret.getText(), textFieldApiKey.getText());
            client.connect();
            profitsChecker.setClient(client);
            coinRatingController.setClient(client);
        }
    }
    
    private void initPairs() {
        pairs.forEach((pair)->{
            pair.set_do_remove_flag(true);
        });
        List<String> items = Arrays.asList(textFieldTradePairs.getText().toUpperCase().split("\\s*,\\s*"));
        if (items.size() > 0) {
            items.forEach((symbol)->{
                if (!symbol.isEmpty()) {
                    boolean has_plus = symbol.contains("+");
                    boolean has_2plus = symbol.contains("++");
                    boolean has_minus = !has_plus && symbol.contains("-");
                    symbol = symbol.replaceAll("\\-", "").replaceAll("\\+", "");
                    int str_index = ComboBoxMainStrategy.getSelectedIndex();
                    if (str_index < 0) {
                        str_index = 0;
                    }
                    int bq_index = comboBoxBarsCount.getSelectedIndex();
                    if (bq_index < 0) {
                        bq_index = 0;
                    }
                    int interval_index = comboBoxBarsInterval.getSelectedIndex();
                    if (interval_index < 0) {
                        interval_index = 0;
                    }
                    int pair_index = searchCurrencyFirstPair(symbol);
                    if (pair_index < 0) {
                        tradePairProcess nproc = new tradePairProcess(this, client, profitsChecker, symbol);
                        nproc.setTryingToSellUp(has_plus);
                        nproc.setSellUpAll(has_2plus);
                        nproc.setTryingToBuyDip(has_minus);
                        nproc.set_do_remove_flag(false);
                        nproc.setTradingBalancePercent((Integer) spinnerBuyPercent.getValue());
                        nproc.setMainStrategy(ComboBoxMainStrategy.getItemAt(str_index));
                        nproc.setBarInterval(comboBoxBarsInterval.getItemAt(interval_index));
                        nproc.setDelayTime((Integer) spinnerUpdateDelay.getValue());
                        nproc.setCheckOtherStrategies(checkBoxCheckOtherStrategies.isSelected());
                        nproc.setStartDelay(pairs.size() * 1000 + 500);
                        nproc.setUseBuyStopLimited(checkBoxBuyStopLimited.isSelected());
                        nproc.setStopBuyLimitTimeout((Integer) spinnerBuyStopLimited.getValue());
                        nproc.setUseSellStopLimited(checkBoxSellStopLimited.isSelected());
                        nproc.setStopSellLimitTimeout((Integer) spinnerSellStopLimited.getValue());
                        nproc.setBarQueryCount(Integer.parseInt(comboBoxBarsCount.getItemAt(bq_index)) / 500);
                        nproc.start();
                        pairs.add(nproc);
                    } else {
                        pairs.get(pair_index).setTryingToBuyDip(has_minus);
                        pairs.get(pair_index).set_do_remove_flag(false);
                        pairs.get(pair_index).setTradingBalancePercent((Integer) spinnerBuyPercent.getValue());
                        pairs.get(pair_index).setMainStrategy(ComboBoxMainStrategy.getItemAt(str_index));
                        pairs.get(pair_index).setBarInterval(comboBoxBarsInterval.getItemAt(interval_index));
                        pairs.get(pair_index).setDelayTime((Integer) spinnerUpdateDelay.getValue());
                        pairs.get(pair_index).setCheckOtherStrategies(checkBoxCheckOtherStrategies.isSelected());
                        pairs.get(pair_index).setUseSellStopLimited(checkBoxBuyStopLimited.isSelected());
                        pairs.get(pair_index).setStopBuyLimitTimeout((Integer) spinnerBuyStopLimited.getValue());
                        pairs.get(pair_index).setUseSellStopLimited(checkBoxSellStopLimited.isSelected());
                        pairs.get(pair_index).setStopSellLimitTimeout((Integer) spinnerSellStopLimited.getValue());
                        pairs.get(pair_index).setBarQueryCount(Integer.parseInt(comboBoxBarsCount.getItemAt(bq_index)) / 500);
                    }
                }
            });
        }
        int i = 0;
        while (i<pairs.size()) {
            if (pairs.get(i).is_do_remove_flag()) {
                pairs.get(i).doStop();
                profitsChecker.removeCurrencyPair(pairs.get(i).getSymbol());
                pairs.remove(i);
            } else {
                i++;
            }
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        logTextarea = new javax.swing.JTextArea();
        buttonRun = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        textFieldTradePairs = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        buttonClear = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        mainTextarea = new javax.swing.JTextArea();
        buttonPause = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        listCurrencies = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        listProfit = new javax.swing.JList<>();
        buttonBuy = new javax.swing.JButton();
        buttonSell = new javax.swing.JButton();
        buttonSetPairs = new javax.swing.JButton();
        buttonUpdate = new javax.swing.JButton();
        buttonCancelLimit = new javax.swing.JButton();
        buttonShowPlot = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        listHeroes = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        textFieldApiSecret = new javax.swing.JTextField();
        textFieldApiKey = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        spinnerUpdateDelay = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        checkboxTestMode = new javax.swing.JCheckBox();
        checkBoxLimitedOrders = new javax.swing.JCheckBox();
        checkBoxBuyStopLimited = new javax.swing.JCheckBox();
        spinnerBuyStopLimited = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        checkBoxSellStopLimited = new javax.swing.JCheckBox();
        spinnerSellStopLimited = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        comboBoxLimitedMode = new javax.swing.JComboBox<>();
        jPanel1 = new javax.swing.JPanel();
        checkBoxStopGain = new javax.swing.JCheckBox();
        checkBoxStopLoss = new javax.swing.JCheckBox();
        spinnerStopLoss = new javax.swing.JSpinner();
        spinnerStopGain = new javax.swing.JSpinner();
        checkBoxLowHold = new javax.swing.JCheckBox();
        checkBoxCheckOtherStrategies = new javax.swing.JCheckBox();
        spinnerBuyPercent = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        ComboBoxMainStrategy = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        comboBoxBarsInterval = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        comboBoxBarsCount = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        checkboxAutoFastorder = new javax.swing.JCheckBox();
        checkBoxAutoAnalyzer = new javax.swing.JCheckBox();
        spinnerScanRatingDelayTime = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        spinnerScanRatingUpdateTime = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        ButtonStatistics = new javax.swing.JButton();
        buttonWebBrowserOpen = new javax.swing.JButton();
        comboBoxRatingSortby = new javax.swing.JComboBox<>();
        comboBoxRatingSort = new javax.swing.JComboBox<>();
        buttonRatingStart = new javax.swing.JButton();
        buttonRatingStop = new javax.swing.JButton();
        buttonRatingCheck = new javax.swing.JButton();
        progressBarRatingAnalPercent = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        logTextarea.setColumns(20);
        logTextarea.setLineWrap(true);
        logTextarea.setRows(5);
        logTextarea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(logTextarea);

        buttonRun.setText("Run");
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });

        buttonStop.setText("Stop");
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });

        textFieldTradePairs.setText("ltceth,rpxeth,xlmeth,neoeth,iotaeth,dasheth,adaeth");

        jLabel1.setText("Trading pairs");

        buttonClear.setText("Clear");
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        mainTextarea.setColumns(20);
        mainTextarea.setLineWrap(true);
        mainTextarea.setRows(5);
        mainTextarea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(mainTextarea);

        buttonPause.setText("Pause");
        buttonPause.setEnabled(false);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });

        listCurrencies.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listCurrencies.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listCurrenciesMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(listCurrencies);

        listProfit.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(listProfit);

        buttonBuy.setText("Buy");
        buttonBuy.setEnabled(false);
        buttonBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBuyActionPerformed(evt);
            }
        });

        buttonSell.setText("Sell");
        buttonSell.setEnabled(false);
        buttonSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSellActionPerformed(evt);
            }
        });

        buttonSetPairs.setText("Set");
        buttonSetPairs.setEnabled(false);
        buttonSetPairs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetPairsActionPerformed(evt);
            }
        });

        buttonUpdate.setText("Update");
        buttonUpdate.setEnabled(false);
        buttonUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpdateActionPerformed(evt);
            }
        });

        buttonCancelLimit.setText("Cancel limit order");
        buttonCancelLimit.setEnabled(false);
        buttonCancelLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelLimitActionPerformed(evt);
            }
        });

        buttonShowPlot.setText("Show plot");
        buttonShowPlot.setEnabled(false);
        buttonShowPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonShowPlotActionPerformed(evt);
            }
        });

        listHeroes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listHeroes.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listHeroesMouseClicked(evt);
            }
        });
        jScrollPane5.setViewportView(listHeroes);

        jLabel6.setText("Log");

        jLabel7.setText("Buy & sell log");

        jLabel8.setText("Coins rating");

        jLabel9.setText("Api Secret / Api Key:");

        jLabel10.setText("Trading currencies / pairs:");

        jLabel11.setText("Market base currencies:");

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        spinnerUpdateDelay.setValue(10);

        jLabel5.setText("Update delay:");

        checkboxTestMode.setSelected(true);
        checkboxTestMode.setText("Test mode");
        checkboxTestMode.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkboxTestModeStateChanged(evt);
            }
        });

        checkBoxLimitedOrders.setText("Limited orders");
        checkBoxLimitedOrders.setEnabled(false);
        checkBoxLimitedOrders.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxLimitedOrdersStateChanged(evt);
            }
        });
        checkBoxLimitedOrders.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxLimitedOrdersActionPerformed(evt);
            }
        });

        checkBoxBuyStopLimited.setText("Stop limited buy orders if waiting more than:");
        checkBoxBuyStopLimited.setActionCommand("Stop limited  buy orders if waiting more than:");
        checkBoxBuyStopLimited.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxBuyStopLimitedStateChanged(evt);
            }
        });

        spinnerBuyStopLimited.setEnabled(false);
        spinnerBuyStopLimited.setValue(120);

        jLabel12.setText("sec.");

        checkBoxSellStopLimited.setText("Stop limited sell orders if waiting more than:");
        checkBoxSellStopLimited.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxSellStopLimitedStateChanged(evt);
            }
        });

        spinnerSellStopLimited.setEnabled(false);
        spinnerSellStopLimited.setValue(120);

        jLabel13.setText("sec.");

        comboBoxLimitedMode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Only Sell", "Sell and Buy" }));
        comboBoxLimitedMode.setEnabled(false);
        comboBoxLimitedMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxLimitedModeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxLimitedOrders)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxLimitedMode, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkboxTestMode)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxBuyStopLimited)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerBuyStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxSellStopLimited)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerSellStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel13)))
                .addContainerGap(162, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spinnerUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21)
                .addComponent(checkboxTestMode)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxLimitedOrders)
                    .addComponent(comboBoxLimitedMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxBuyStopLimited)
                    .addComponent(spinnerBuyStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxSellStopLimited)
                    .addComponent(spinnerSellStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addContainerGap(41, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Main", jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        checkBoxStopGain.setText("Stop Gain percent:");
        checkBoxStopGain.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxStopGainStateChanged(evt);
            }
        });

        checkBoxStopLoss.setText("Stop Loss percent:");
        checkBoxStopLoss.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxStopLossStateChanged(evt);
            }
        });

        spinnerStopLoss.setEnabled(false);
        spinnerStopLoss.setValue(3);

        spinnerStopGain.setEnabled(false);
        spinnerStopGain.setValue(30);

        checkBoxLowHold.setSelected(true);
        checkBoxLowHold.setText("Hold on low profit");
        checkBoxLowHold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxLowHoldStateChanged(evt);
            }
        });

        checkBoxCheckOtherStrategies.setText("Check other strategies");

        spinnerBuyPercent.setValue(100);

        jLabel3.setText("Buy %:");

        ComboBoxMainStrategy.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Auto" }));

        jLabel4.setText("Main strategy:");

        comboBoxBarsInterval.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1m", "5m", "15m", "30m", "1h", "2h" }));

        jLabel2.setText("Bars interval:");

        jLabel14.setText("Bars count:");

        comboBoxBarsCount.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "500", "1000", "1500", "2000", "3000", "4000", "5000" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxCheckOtherStrategies)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(checkBoxStopGain)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerStopGain, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(checkBoxStopLoss)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerStopLoss, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(ComboBoxMainStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(spinnerBuyPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(comboBoxBarsInterval, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(comboBoxBarsCount, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel14)))
                            .addComponent(checkBoxLowHold))))
                .addContainerGap(117, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(8, 8, 8)
                        .addComponent(ComboBoxMainStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(8, 8, 8)
                        .addComponent(spinnerBuyPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxBarsInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxBarsCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkBoxCheckOtherStrategies)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxStopLoss)
                    .addComponent(spinnerStopLoss, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxStopGain)
                    .addComponent(spinnerStopGain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLowHold)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Strategies", jPanel1);

        checkboxAutoFastorder.setText("Auto order");
        checkboxAutoFastorder.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkboxAutoFastorderStateChanged(evt);
            }
        });
        checkboxAutoFastorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxAutoFastorderActionPerformed(evt);
            }
        });

        checkBoxAutoAnalyzer.setText("Auto analyzer");

        spinnerScanRatingDelayTime.setValue(2);
        spinnerScanRatingDelayTime.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScanRatingDelayTimeStateChanged(evt);
            }
        });

        jLabel15.setText("Next coin check delay time:");

        jLabel16.setText("Update time:");

        spinnerScanRatingUpdateTime.setValue(10);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxAutoAnalyzer)
                    .addComponent(checkboxAutoFastorder)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(spinnerScanRatingDelayTime, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .addComponent(spinnerScanRatingUpdateTime))))
                .addContainerGap(263, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBoxAutoAnalyzer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkboxAutoFastorder)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerScanRatingDelayTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(spinnerScanRatingUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(79, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Coins rating", jPanel4);

        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Modifiers:\nBuy for best price: -\nSell free ordered (or opened orders if no free): +\nSell all (free + opened orders): ++");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane6.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Tips", jPanel3);

        ButtonStatistics.setText("Statistics");
        ButtonStatistics.setEnabled(false);
        ButtonStatistics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonStatisticsActionPerformed(evt);
            }
        });

        buttonWebBrowserOpen.setText("Binance");
        buttonWebBrowserOpen.setEnabled(false);
        buttonWebBrowserOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonWebBrowserOpenActionPerformed(evt);
            }
        });

        comboBoxRatingSortby.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Rank", "Market cap", "% from prog start", "% last hour", "% 24hr", "Events count", "Last event anno date", "Volatility", "Calculated rating" }));
        comboBoxRatingSortby.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxRatingSortbyActionPerformed(evt);
            }
        });

        comboBoxRatingSort.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ASC", "DESC" }));
        comboBoxRatingSort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxRatingSortActionPerformed(evt);
            }
        });

        buttonRatingStart.setText("Start");
        buttonRatingStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRatingStartActionPerformed(evt);
            }
        });

        buttonRatingStop.setText("Stop");
        buttonRatingStop.setEnabled(false);
        buttonRatingStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRatingStopActionPerformed(evt);
            }
        });

        buttonRatingCheck.setText("Check");
        buttonRatingCheck.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(buttonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel9))
                            .addGap(161, 161, 161))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                                .addComponent(jScrollPane1)
                                .addComponent(jLabel7)
                                .addComponent(textFieldApiKey, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(textFieldApiSecret, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel8)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 209, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(comboBoxRatingSortby, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxRatingSort, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(progressBarRatingAnalPercent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(textFieldTradePairs)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSetPairs))
                            .addComponent(jTabbedPane2)
                            .addComponent(jScrollPane3)
                            .addComponent(jScrollPane4)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(buttonRun, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonStop, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonPause, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel11))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buttonWebBrowserOpen)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(ButtonStatistics))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(buttonBuy)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSell)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCancelLimit)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonUpdate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonShowPlot))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonRatingStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRatingStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRatingCheck)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldTradePairs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonSetPairs))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonRun)
                            .addComponent(buttonStop)
                            .addComponent(buttonPause))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonBuy)
                            .addComponent(buttonSell)
                            .addComponent(buttonUpdate)
                            .addComponent(buttonCancelLimit)
                            .addComponent(buttonShowPlot))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ButtonStatistics)
                            .addComponent(buttonWebBrowserOpen))
                        .addGap(4, 4, 4))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(comboBoxRatingSortby, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(comboBoxRatingSort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(progressBarRatingAnalPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(textFieldApiSecret, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(textFieldApiKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(9, 9, 9)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonClear)
                    .addComponent(buttonRatingStart)
                    .addComponent(buttonRatingStop)
                    .addComponent(buttonRatingCheck)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        log("Starting...\n", true, true);
        is_paused = false;
        buttonRun.setEnabled(false);
        try {
            initAPI();
            profitsChecker.setTestMode(checkboxTestMode.isSelected());
            profitsChecker.setLimitedOrders(checkBoxLimitedOrders.isSelected());
            profitsChecker.setLimitedOrderMode(comboBoxLimitedMode.getSelectedIndex() == 0 ? tradeProfitsController.LimitedOrderMode.LOMODE_SELL : tradeProfitsController.LimitedOrderMode.LOMODE_SELLANDBUY);
            profitsChecker.setStopGainPercent(checkBoxStopGain.isSelected() ? BigDecimal.valueOf((Integer) spinnerStopGain.getValue()) : null);
            profitsChecker.setStopLossPercent(checkBoxStopLoss.isSelected() ? BigDecimal.valueOf((Integer) spinnerStopLoss.getValue()) : null);
            profitsChecker.setLowHold(checkBoxLowHold.isSelected());
            profitsChecker.showTradeComissionCurrency();

            /*log("ServerTime = " + client.getServerTime());
            log("");
            log("Balances:", true);
            Account account = profitsChecker.getAccount();
            List<AssetBalance> allBalances = account.getBalances();        
            allBalances.forEach((balance)->{
                if (Float.parseFloat(balance.getFree()) > 0 || Float.parseFloat(balance.getLocked()) > 0) {
                    log(balance.getAsset() + " = " + balance.getFree() + " / " + balance.getLocked(), true);
                }
            });
            
            log("", true);
            log("Limits:", true);
            List<RateLimit> limits = client.getExchangeInfo().getRateLimits();
            limits.forEach((limit)->{
                log(limit.getRateLimitType().name() + " " + limit.getInterval().name() + " " + limit.getLimit(), true);
            });*/

            log("", true);
            initPairs();

            checkboxTestMode.setEnabled(false);
            buttonStop.setEnabled(true);
            buttonPause.setEnabled(true);
            buttonBuy.setEnabled(true);
            buttonSell.setEnabled(true);
            buttonSetPairs.setEnabled(true);
            buttonUpdate.setEnabled(!checkboxTestMode.isSelected());
            buttonCancelLimit.setEnabled(checkBoxLimitedOrders.isSelected());
            buttonShowPlot.setEnabled(true);
            ButtonStatistics.setEnabled(true);
            buttonWebBrowserOpen.setEnabled(true);
        } catch (Exception e) {
            log("Error: " + e.getMessage(), true, true);
            buttonRun.setEnabled(true);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        is_paused = false;
        buttonStop.setEnabled(false);
        buttonPause.setEnabled(false);
        if (pairs.size() > 0) {
            pairs.forEach((pair)->{
                pair.doStop();
            });
            pairs.clear();
        }
        listHeroes.setModel(new DefaultListModel<>());
        buttonRun.setEnabled(true);
        checkboxTestMode.setEnabled(true);
        buttonBuy.setEnabled(false);
        buttonSell.setEnabled(false);
        buttonSetPairs.setEnabled(false);
        buttonUpdate.setEnabled(false);
        buttonCancelLimit.setEnabled(false);
        buttonShowPlot.setEnabled(false);
        ButtonStatistics.setEnabled(false);
        buttonWebBrowserOpen.setEnabled(false);
    }//GEN-LAST:event_buttonStopActionPerformed

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        logTextarea.setText("");
        mainTextarea.setText("");
    }//GEN-LAST:event_buttonClearActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        is_paused = !is_paused;
        buttonStop.setEnabled(true);
        buttonPause.setEnabled(true);
        buttonRun.setEnabled(false);
        buttonBuy.setEnabled(true);
        buttonSell.setEnabled(true);
        checkboxTestMode.setEnabled(false);
        buttonSetPairs.setEnabled(true);
        buttonUpdate.setEnabled(!checkboxTestMode.isSelected());
        buttonCancelLimit.setEnabled(checkBoxLimitedOrders.isSelected());
        buttonShowPlot.setEnabled(true);
        log(is_paused ? "Paused..." : "Unpaused");
        if (pairs.size() > 0) {
            pairs.forEach((pair)->{
                pair.doSetPaused(is_paused);
            });
        }
        coinRatingController.doSetPaused(is_paused);
        ButtonStatistics.setEnabled(true);
        buttonWebBrowserOpen.setEnabled(true);
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBuyActionPerformed
        if (pairs.size() > 0) {
            int curr_index = listCurrencies.getSelectedIndex();
            if (curr_index >= 0) {
                String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                int pair_index = searchCurrencyFirstPair(currencyPair, false);
                if (pair_index >= 0 && pair_index < pairs.size()) {
                    pairs.get(pair_index).doBuy();
                }
            }
        }
    }//GEN-LAST:event_buttonBuyActionPerformed

    private void buttonSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSellActionPerformed
        if (pairs.size() > 0) {
            int curr_index = listCurrencies.getSelectedIndex();
            if (curr_index >= 0) {
                String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                int pair_index = searchCurrencyFirstPair(currencyPair, true);
                if (pair_index >= 0 && pair_index < pairs.size()) {
                    pairs.get(pair_index).doSell();
                }
            }
        }
    }//GEN-LAST:event_buttonSellActionPerformed

    private void buttonSetPairsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetPairsActionPerformed
        profitsChecker.setLimitedOrders(checkBoxLimitedOrders.isSelected());
        profitsChecker.setStopGainPercent(checkBoxStopGain.isSelected() ? BigDecimal.valueOf((Integer) spinnerStopGain.getValue()) : null);
        profitsChecker.setStopLossPercent(checkBoxStopLoss.isSelected() ? BigDecimal.valueOf((Integer) spinnerStopLoss.getValue()) : null);
        profitsChecker.setLowHold(checkBoxLowHold.isSelected());
        initPairs();
    }//GEN-LAST:event_buttonSetPairsActionPerformed

    private void buttonUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpdateActionPerformed
        if (pairs.size() > 0) {
            profitsChecker.updateAllBalances();
        }
    }//GEN-LAST:event_buttonUpdateActionPerformed

    private void buttonCancelLimitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelLimitActionPerformed
        if (pairs.size() > 0) {
            int curr_index = listCurrencies.getSelectedIndex();
            if (curr_index >= 0) {
                String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                int pair_index = searchCurrencyFirstPair(currencyPair);
                if (pair_index >= 0 && pair_index < pairs.size()) {
                    pairs.get(pair_index).doLimitCancel();
                }
            }
        }
    }//GEN-LAST:event_buttonCancelLimitActionPerformed

    private void buttonShowPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShowPlotActionPerformed
        if (pairs.size() > 0) {
            int curr_index = listCurrencies.getSelectedIndex();
            if (curr_index >= 0) {
                String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                int pair_index = searchCurrencyFirstPair(currencyPair);
                if (pair_index >= 0 && pair_index < pairs.size()) {
                    pairs.get(pair_index).doShowPlot();
                }
            }
        }
    }//GEN-LAST:event_buttonShowPlotActionPerformed

    private void checkBoxLimitedOrdersStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxLimitedOrdersStateChanged
        if (profitsChecker != null) {
            profitsChecker.setLimitedOrders(checkBoxLimitedOrders.isSelected());
        }
    }//GEN-LAST:event_checkBoxLimitedOrdersStateChanged

    private void listHeroesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listHeroesMouseClicked
        if (evt.getClickCount() == 2) {
            String content = listHeroes.getSelectedValue();
            if (content != null && !content.isEmpty()) {
                String[] parts = content.split(":");
                if (parts.length > 0) {
                    String newtext = parts[0].toLowerCase();
                    if (!textFieldTradePairs.getText().isEmpty()) {
                        newtext = textFieldTradePairs.getText() + "," + newtext;
                    }
                    textFieldTradePairs.setText(newtext);
                }
            }
        }
    }//GEN-LAST:event_listHeroesMouseClicked

    private void listCurrenciesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listCurrenciesMouseClicked
        if (evt.getClickCount() == 2) {
            buttonShowPlotActionPerformed(null);
        }
    }//GEN-LAST:event_listCurrenciesMouseClicked

    private void checkboxTestModeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkboxTestModeStateChanged
        checkBoxLimitedOrders.setEnabled(!checkboxTestMode.isSelected());
        comboBoxLimitedMode.setEnabled(checkBoxLimitedOrders.isSelected() && !checkboxTestMode.isSelected());
    }//GEN-LAST:event_checkboxTestModeStateChanged

    private void checkBoxLowHoldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxLowHoldStateChanged
        if (coinRatingController != null) {
            coinRatingController.setLowHold(checkBoxLowHold.isSelected());
        }
    }//GEN-LAST:event_checkBoxLowHoldStateChanged

    private void checkboxAutoFastorderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkboxAutoFastorderStateChanged
        if (coinRatingController != null) {
            coinRatingController.setAutoOrder(checkboxAutoFastorder.isSelected());
        }
    }//GEN-LAST:event_checkboxAutoFastorderStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        prefs.put("window_pos_x", String.valueOf(Math.round(getBounds().getX())));
        prefs.put("window_pos_y", String.valueOf(Math.round(getBounds().getY())));
        prefs.put("window_size_x", String.valueOf(Math.round(getBounds().getWidth())));
        prefs.put("window_size_y", String.valueOf(Math.round(getBounds().getHeight())));
        componentPrefSave(textFieldApiKey, "api_key");
        componentPrefSave(textFieldApiSecret, "api_secret");
        componentPrefSave(textFieldTradePairs, "trade_pairs");
        componentPrefSave(checkboxTestMode, "test_mode");
        componentPrefSave(checkBoxLowHold, "low_hold");
        componentPrefSave(checkboxAutoFastorder, "auto_heroes");
        componentPrefSave(checkBoxAutoAnalyzer, "auto_anal");
        componentPrefSave(checkBoxLimitedOrders, "limited_orders");
        componentPrefSave(checkBoxCheckOtherStrategies, "strategies_add_check");
        componentPrefSave(spinnerUpdateDelay, "update_delay");
        componentPrefSave(spinnerBuyPercent, "buy_percent");
        componentPrefSave(comboBoxBarsInterval, "bars");
        componentPrefSave(comboBoxBarsCount, "bars_queries_index");
        componentPrefSave(ComboBoxMainStrategy, "main_strategy");
        componentPrefSave(checkBoxStopGain, "use_stop_gain");
        componentPrefSave(checkBoxStopLoss, "use_stop_loss");
        componentPrefSave(spinnerStopGain, "stop_gain");
        componentPrefSave(spinnerStopLoss, "stop_loss");
        componentPrefSave(checkBoxBuyStopLimited, "use_buy_stop_limited_timeout");
        componentPrefSave(spinnerBuyStopLimited, "buy_stop_limited_timeout");
        componentPrefSave(checkBoxSellStopLimited, "use_sell_stop_limited_timeout");
        componentPrefSave(spinnerSellStopLimited, "sell_stop_limited_timeout");
        componentPrefSave(comboBoxLimitedMode, "limited_mode");
    }//GEN-LAST:event_formWindowClosing

    private void checkBoxStopLossStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxStopLossStateChanged
        spinnerStopLoss.setEnabled(checkBoxStopLoss.isSelected());
    }//GEN-LAST:event_checkBoxStopLossStateChanged

    private void checkBoxStopGainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxStopGainStateChanged
        spinnerStopGain.setEnabled(checkBoxStopGain.isSelected());
    }//GEN-LAST:event_checkBoxStopGainStateChanged

    private void checkBoxBuyStopLimitedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxBuyStopLimitedStateChanged
        spinnerBuyStopLimited.setEnabled(checkBoxBuyStopLimited.isSelected());
    }//GEN-LAST:event_checkBoxBuyStopLimitedStateChanged

    private void checkBoxSellStopLimitedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxSellStopLimitedStateChanged
        spinnerSellStopLimited.setEnabled(checkBoxSellStopLimited.isSelected());
    }//GEN-LAST:event_checkBoxSellStopLimitedStateChanged

    private void ButtonStatisticsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonStatisticsActionPerformed
        if (pairs.size() > 0) {
            int curr_index = listCurrencies.getSelectedIndex();
            if (curr_index >= 0) {
                String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                int pair_index = searchCurrencyFirstPair(currencyPair);
                if (pair_index >= 0 && pair_index < pairs.size()) {
                    pairs.get(pair_index).doShowStatistics();
                }
            }
        }
    }//GEN-LAST:event_ButtonStatisticsActionPerformed

    private void buttonWebBrowserOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonWebBrowserOpenActionPerformed
        if (Desktop.isDesktopSupported()) {
            try {
                if (pairs.size() > 0) {
                    int curr_index = listCurrencies.getSelectedIndex();
                    if (curr_index >= 0) {
                        String currencyPair = profitsChecker.getNthCurrencyPair(curr_index);
                        int pair_index = searchCurrencyFirstPair(currencyPair);
                        if (pair_index >= 0 && pair_index < pairs.size()) {
                            String url = "https://www.binance.com/tradeDetail.html?symbol="+pairs.get(pair_index).getBaseSymbol()+"_"+pairs.get(pair_index).getQuoteSymbol();
                            Desktop.getDesktop().browse(new URI(url));
                        }
                    }
                }
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(mainApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_buttonWebBrowserOpenActionPerformed

    private void checkBoxLimitedOrdersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxLimitedOrdersActionPerformed
        comboBoxLimitedMode.setEnabled(checkBoxLimitedOrders.isSelected());
    }//GEN-LAST:event_checkBoxLimitedOrdersActionPerformed

    private void comboBoxLimitedModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxLimitedModeActionPerformed
        if (profitsChecker != null) {
            profitsChecker.setLimitedOrderMode(comboBoxLimitedMode.getSelectedIndex() == 0 ? tradeProfitsController.LimitedOrderMode.LOMODE_SELL : tradeProfitsController.LimitedOrderMode.LOMODE_SELLANDBUY);
        }
    }//GEN-LAST:event_comboBoxLimitedModeActionPerformed

    private void checkboxAutoFastorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxAutoFastorderActionPerformed
        if (coinRatingController != null) {
            coinRatingController.setAutoOrder(checkboxAutoFastorder.isSelected());
        }
    }//GEN-LAST:event_checkboxAutoFastorderActionPerformed

    private void buttonRatingStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRatingStartActionPerformed
        initAPI();
        coinRatingController.setLowHold(checkBoxLowHold.isSelected());
        coinRatingController.setAutoOrder(checkboxAutoFastorder.isSelected());
        coinRatingController.setAnalyzer(checkBoxAutoAnalyzer.isSelected());
        coinRatingController.setDelayTime((Integer) spinnerScanRatingDelayTime.getValue());
        coinRatingController.setUpdateTime((Integer) spinnerScanRatingUpdateTime.getValue());
        coinRatingController.setProgressBar(progressBarRatingAnalPercent);
        comboBoxRatingSortActionPerformed(null);
        comboBoxRatingSortbyActionPerformed(null);
        coinRatingController.start();
        buttonRatingStart.setEnabled(false);
        buttonRatingCheck.setEnabled(true);
        buttonRatingStop.setEnabled(true);
    }//GEN-LAST:event_buttonRatingStartActionPerformed

    private void buttonRatingStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRatingStopActionPerformed
        coinRatingController.doStop();
        buttonRatingStart.setEnabled(true);
        buttonRatingCheck.setEnabled(false);
        buttonRatingStop.setEnabled(false);
    }//GEN-LAST:event_buttonRatingStopActionPerformed

    private void comboBoxRatingSortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxRatingSortActionPerformed
        coinRatingController.setSortAsc(comboBoxRatingSort.getSelectedIndex() == 0);
    }//GEN-LAST:event_comboBoxRatingSortActionPerformed

    private void comboBoxRatingSortbyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxRatingSortbyActionPerformed
        int sindex = comboBoxRatingSortby.getSelectedIndex();
        if (sindex < 0) sindex = 0;
        else if (sindex > CoinRatingController.CoinRatingSort.values().length-1) sindex = CoinRatingController.CoinRatingSort.values().length-1;
        coinRatingController.setSortby(CoinRatingController.CoinRatingSort.values()[sindex]);
    }//GEN-LAST:event_comboBoxRatingSortbyActionPerformed

    private void spinnerScanRatingDelayTimeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScanRatingDelayTimeStateChanged
        coinRatingController.setDelayTime((Integer) spinnerScanRatingDelayTime.getValue());
    }//GEN-LAST:event_spinnerScanRatingDelayTimeStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(mainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(mainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(mainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(mainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new mainApplication().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ButtonStatistics;
    private javax.swing.JComboBox<String> ComboBoxMainStrategy;
    private javax.swing.JButton buttonBuy;
    private javax.swing.JButton buttonCancelLimit;
    private javax.swing.JButton buttonClear;
    private javax.swing.JButton buttonPause;
    private javax.swing.JButton buttonRatingCheck;
    private javax.swing.JButton buttonRatingStart;
    private javax.swing.JButton buttonRatingStop;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonSell;
    private javax.swing.JButton buttonSetPairs;
    private javax.swing.JButton buttonShowPlot;
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton buttonUpdate;
    private javax.swing.JButton buttonWebBrowserOpen;
    private javax.swing.JCheckBox checkBoxAutoAnalyzer;
    private javax.swing.JCheckBox checkBoxBuyStopLimited;
    private javax.swing.JCheckBox checkBoxCheckOtherStrategies;
    private javax.swing.JCheckBox checkBoxLimitedOrders;
    private javax.swing.JCheckBox checkBoxLowHold;
    private javax.swing.JCheckBox checkBoxSellStopLimited;
    private javax.swing.JCheckBox checkBoxStopGain;
    private javax.swing.JCheckBox checkBoxStopLoss;
    private javax.swing.JCheckBox checkboxAutoFastorder;
    private javax.swing.JCheckBox checkboxTestMode;
    private javax.swing.JComboBox<String> comboBoxBarsCount;
    private javax.swing.JComboBox<String> comboBoxBarsInterval;
    private javax.swing.JComboBox<String> comboBoxLimitedMode;
    private javax.swing.JComboBox<String> comboBoxRatingSort;
    private javax.swing.JComboBox<String> comboBoxRatingSortby;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JList<String> listCurrencies;
    private javax.swing.JList<String> listHeroes;
    private javax.swing.JList<String> listProfit;
    private javax.swing.JTextArea logTextarea;
    private javax.swing.JTextArea mainTextarea;
    private javax.swing.JProgressBar progressBarRatingAnalPercent;
    private javax.swing.JSpinner spinnerBuyPercent;
    private javax.swing.JSpinner spinnerBuyStopLimited;
    private javax.swing.JSpinner spinnerScanRatingDelayTime;
    private javax.swing.JSpinner spinnerScanRatingUpdateTime;
    private javax.swing.JSpinner spinnerSellStopLimited;
    private javax.swing.JSpinner spinnerStopGain;
    private javax.swing.JSpinner spinnerStopLoss;
    private javax.swing.JSpinner spinnerUpdateDelay;
    private javax.swing.JTextField textFieldApiKey;
    private javax.swing.JTextField textFieldApiSecret;
    private javax.swing.JTextField textFieldTradePairs;
    // End of variables declaration//GEN-END:variables

}
