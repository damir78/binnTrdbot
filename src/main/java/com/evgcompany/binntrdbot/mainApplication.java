/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot;

import com.evgcompany.binntrdbot.api.TradingAPIAbstractInterface;
import com.evgcompany.binntrdbot.api.TradingAPIBinance;
import com.evgcompany.binntrdbot.coinrating.*;
import com.evgcompany.binntrdbot.misc.ComponentsConfigController;
import com.evgcompany.binntrdbot.misc.NumberFormatter;
import com.evgcompany.binntrdbot.signal.SignalController;
import com.evgcompany.binntrdbot.strategies.core.StrategiesController;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;

/**
 *
 * @author EVG_adm_T
 */
public class mainApplication extends javax.swing.JFrame {

    private final LogsForm logsForm = new LogsForm();
    
    private TradingAPIAbstractInterface client = null;
    
    private ComponentsConfigController config = null;
    private TradePairProcessList pairProcessController = null;
    private CoinRatingController coinRatingController = null;
    
    private boolean is_paused = false;
    
    private static volatile mainApplication instance = null;
    
    public static mainApplication getInstance() {
        return instance;
    }
    
    public void systemSound() {
        final Runnable runnable =
            (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
       if (runnable != null) runnable.run();
    }
    
    /**
     * Creates new form mainApplication
     */
    public mainApplication() {
        instance = this;
        config = new ComponentsConfigController(this);
        pairProcessController = new TradePairProcessList();
        coinRatingController = new CoinRatingController(this, pairProcessController);
        initComponents();
        listProfit.setModel(BalanceController.getInstance().getListProfitModel());
        listCurrencies.setModel(OrdersController.getInstance().getListPairOrdersModel());
        listRating.setModel(coinRatingController.getCoinRatingModel());
        listBoxAutoStrategies.setModel(new DefaultListModel<>());
        new StrategiesController().getStrategiesNames().forEach((strategy_name)->{
            ComboBoxMainStrategy.addItem(strategy_name);
            ((DefaultListModel)listBoxAutoStrategies.getModel()).addElement(strategy_name);
        });
        
        coinRatingController.setTrendUpdateEvent((upt, dnt) -> {
            labelUpTrend.setText("UP: " + NumberFormatter.df2.format(upt) + "%");
            labelDownTrend.setText("DOWN: " + NumberFormatter.df2.format(dnt) + "%");
        });
        coinRatingController.setMarketcapUpdateEvent((volume, cap, dom) -> {
            labelMCVolume.setText(NumberFormatter.formatVolume(volume) + "$" + " ("+NumberFormatter.formatPercentChange(volume,coinRatingController.getInitialVol24())+")");
            labelMCCap.setText(NumberFormatter.formatVolume(cap) + "$" + " ("+NumberFormatter.formatPercentChange(cap,coinRatingController.getInitialMarketCap())+")");
            labelMCBTCDominance.setText(NumberFormatter.df3.format(dom) + "%" + " ("+NumberFormatter.formatOffsetChange(dom,coinRatingController.getInitialBTCDominance())+"%)");
        });
        
        BalanceController.getInstance().setAccountCostUpdate((agg)->{
            double initialCost = agg.getInitialAccountCost();
            if (initialCost >= 0) {
                double curCost = agg.getBaseAccountCost();
                String cost_text = "Account cost: " + NumberFormatter.df6.format(curCost) + " " + CoinInfoAggregator.getInstance().getBaseCoin() + " ("+NumberFormatter.formatPercentChange(curCost, initialCost)+")";
                labelAccountCost.setText(cost_text);
            }
        });
        
        config.addComponent(instance, "window");
        config.addComponent(logsForm, "log_window");
        config.addComponent(textFieldApiKey, "api_key");
        config.addComponent(textFieldApiSecret, "api_secret");
        config.addComponent(textFieldTradePairs, "trade_pairs");
        config.addComponent(checkboxTestMode, "test_mode");
        config.addComponent(checkBoxLowHold, "low_hold");
        config.addComponent(checkboxAutoOrder, "auto_order");
        config.addComponent(checkboxAutoFastorder, "auto_fastorder");
        config.addComponent(checkBoxAutoAnalyzer, "auto_anal");
        config.addComponent(checkBoxLimitedOrders, "limited_orders");
        config.addComponent(checkBoxCheckOtherStrategies, "strategies_add_check");
        config.addComponent(spinnerUpdateDelay, "update_delay");
        config.addComponent(spinnerBuyPercent, "pair_order_percent");
        config.addComponent(spinnerBuyMainValue, "pair_order_main_value");
        config.addComponent(comboBoxBarsInterval, "bars");
        config.addComponent(comboBoxBarsCount, "bars_queries_index");
        config.addComponent(ComboBoxMainStrategy, "main_strategy");
        config.addComponent(checkBoxStopGain, "use_stop_gain");
        config.addComponent(checkBoxStopLoss, "use_stop_loss");
        config.addComponent(spinnerStopGain, "stop_gain");
        config.addComponent(spinnerStopLoss, "stop_loss");
        config.addComponent(checkBoxBuyStopLimited, "use_buy_stop_limited_timeout");
        config.addComponent(spinnerBuyStopLimited, "buy_stop_limited_timeout");
        config.addComponent(checkBoxSellStopLimited, "use_sell_stop_limited_timeout");
        config.addComponent(spinnerSellStopLimited, "sell_stop_limited_timeout");
        config.addComponent(comboBoxLimitedMode, "limited_mode");
        config.addComponent(textFieldAPIID, "signals_api_id");
        config.addComponent(textFieldAPIHash, "signals_api_hash");
        config.addComponent(textFieldPhone, "signals_api_phone");
        config.addComponent(checkboxAutoSignalOrder, "auto_signals_order");
        config.addComponent(checkboxAutoSignalFastorder, "auto_signals_fast_order");
        config.addComponent(spinnerScanRatingDelayTime, "rating_scan_delay_time");
        config.addComponent(spinnerScanRatingUpdateTime, "rating_scan_update_time");
        config.addComponent(spinnerScanTrendUpdateTime, "rating_scan_trend_update_time");
        config.addComponent(spinnerScanRanksUpdateTime, "rating_scan_ranks_update_time");
        config.addComponent(spinnerRatingMaxOrders, "rating_max_order_count");
        config.addComponent(spinnerRatingMaxOrderWait, "rating_max_order_wait");
        config.addComponent(spinnerRatingMinForOrder, "rating_min_for_order");
        config.addComponent(listBoxAutoStrategies, "auto_strategies_list");
        config.addComponent(checkBoxWalkForward, "auto_walkforward");
        config.addComponent(checkBoxAutoStrategyParams, "auto_strategyparamspick");
        config.addComponent(spinnerSignalRatingMinForOrder, "signal_rating_min_for_order");
        config.addComponent(spinnerSignalPreloadCount, "signal_preload_count");
        config.addComponent(checkBoxDowntrendNoAuto, "rating_no_auto_on_downtrend");
        config.addComponent(checkBoxUseSignals, "signals_use");
        config.addComponent(checkBoxUseCycles, "cycles_use");
        config.addComponent(textFieldBaseCoin, "base_coin");
        config.addComponent(spinnerRatingMinBaseVolume, "cycle_basecoin_minvolume");
        config.addComponent(spinnerPricesUpdateDelay, "prices_update_delay");
        config.addComponent(textFieldRestrictedCoins, "cycle_restricted_coins");
        config.addComponent(textFieldRequiredCoins, "cycle_required_coins");
        config.addComponent(textFieldCycleMainCoins, "cycle_main_coins");
        config.addComponent(spinnerCycleDelay, "cycle_delay");
        config.addComponent(spinnerCyclePercent, "cycle_buy_percent");
        config.addComponent(spinnerCycleAbortTime, "cycle_abort_time");
        config.addComponent(spinnerCycleFirstAbortTime, "cycle_first_abort_time");
        config.addComponent(spinnerCycleMinProfitPercent, "cycle_minprofit");
        config.addComponent(spinnerCycleMaxCoinRank, "cycle_maxrank");
        config.addComponent(spinnerCycleMinPairRating, "cycle_minpair_rating");
        config.addComponent(spinnerCycleMinBaseVolume, "cycle_minbase_volume");
        config.addComponent(spinnerCycleSwitchTime, "cycle_switch_time");
        config.addComponent(spinnerCycleMaxSwitches, "cycle_switch_maxcount");
        config.addComponent(spinnerCycleMaxEnterCount, "cycle_maxcount");
        config.addComponent(checkBoxCycleDepthCheck, "cycle_depset");
        config.addComponent(checkBoxWavesUse, "waves_use");
        config.addComponent(spinnerBuyWavesFirstPercent, "waves_buy_first_percent");
        config.addComponent(spinnerBuyWavesFirstMainValue, "waves_buy_first_main");
        config.addComponent(spinnerBuyWavesSecondPercent, "waves_buy_second_percent");
        config.addComponent(spinnerBuyWavesIncKoef, "waves_buy_inc_koef");
        config.addComponent(spinnerWavesMinProfitPercent, "waves_min_profit_percent");
        config.addComponent(spinnerWavesInitialProfitPercent, "waves_initial_profit_percent");
        config.addComponent(spinnerWavesHalfProfitOrdersCnt, "waves_half_profit_orders");
        config.addComponent(checkBoxTestAdd, "test_useadd");
        config.addComponent(textFieldTestCoinAdd, "test_addcoin");
        config.addComponent(spinnerTestAdd, "test_addcoinval");
        config.addComponent(checkBoxPyramiding, "orders_pyramiding");
        config.addComponent(spinnerPyramidingMaxC, "orders_pyramidingMaxC");
        config.Load();
    }
    
    public void log(String txt) {
        logsForm.log(txt);
    }
    public void log(String txt, boolean is_main, boolean with_date) {
        logsForm.log(txt, is_main, with_date);
    }
    public void log(String txt, boolean is_main) {
        logsForm.log(txt, is_main);
    }
    
    public TradePairProcessList getPairProcessController() {
        return pairProcessController;
    }
    
    public CoinRatingController getCoinRatingController() {
        return coinRatingController;
    }

    private void initAPI() {
        if (client == null) {
            client = new TradingAPIBinance(textFieldApiSecret.getText(), textFieldApiKey.getText());
            client.connect();
            OrdersController.getInstance().setClient(client);
            BalanceController.getInstance().setClient(client);
            CoinInfoAggregator.getInstance().setClient(client);
            coinRatingController.setClient(client);
        }
        CoinInfoAggregator.getInstance().setBaseCoin(textFieldBaseCoin.getText());
        CoinInfoAggregator.getInstance().setBaseCoinMinCount(((Number) spinnerRatingMinBaseVolume.getValue()).doubleValue());
        CoinInfoAggregator.getInstance().setDelayTime(((Number) spinnerPricesUpdateDelay.getValue()).longValue());
    }
    
    private void setPairParams() {
        int str_index = ComboBoxMainStrategy.getSelectedIndex();
        if (str_index < 0) str_index = 0;
        pairProcessController.setMainStrategy(ComboBoxMainStrategy.getItemAt(str_index));
        int bq_index = comboBoxBarsCount.getSelectedIndex();
        if (bq_index < 0) bq_index = 0;
        pairProcessController.setBarAdditionalCount(Integer.parseInt(comboBoxBarsCount.getItemAt(bq_index)) / 500);
        int interval_index = comboBoxBarsInterval.getSelectedIndex();
        if (interval_index < 0) interval_index = 0;
        pairProcessController.setBarsInterval(comboBoxBarsInterval.getItemAt(interval_index));
        pairProcessController.setTradingBalancePercent(((Number) spinnerBuyPercent.getValue()).intValue());
        pairProcessController.setTradingBalanceMainValue(((Number) spinnerBuyMainValue.getValue()).doubleValue());
        pairProcessController.setBuyStop(checkBoxBuyStopLimited.isSelected());
        pairProcessController.setSellStop(checkBoxSellStopLimited.isSelected());
        pairProcessController.setBuyStopLimitedTimeout(((Number) spinnerBuyStopLimited.getValue()).intValue());
        pairProcessController.setSellStopLimitedTimeout(((Number) spinnerSellStopLimited.getValue()).intValue());
        pairProcessController.setUpdateDelay(((Number) spinnerUpdateDelay.getValue()).longValue());
        pairProcessController.setCheckOtherStrategies(checkBoxCheckOtherStrategies.isSelected());
        pairProcessController.setPyramidAutoMaxSize(checkBoxPyramiding.isSelected() ? ((Number) spinnerPyramidingMaxC.getValue()).intValue() : 1);
        pairProcessController.setAllPairsWavesUsage(checkBoxWavesUse.isSelected());
        
        pairProcessController.setWavesMinProfitPercent(((Number) spinnerWavesMinProfitPercent.getValue()).doubleValue());
        pairProcessController.setWavesInitialProfitPercent(((Number) spinnerWavesInitialProfitPercent.getValue()).doubleValue());
        pairProcessController.setWavesHalfDivideProfitOrdersCount(((Number) spinnerWavesHalfProfitOrdersCnt.getValue()).doubleValue());
        pairProcessController.setWavesTradingBalancePercent(((Number) spinnerBuyWavesFirstPercent.getValue()).doubleValue());
        pairProcessController.setWavesTradingBalanceMainValue(((Number) spinnerBuyWavesFirstMainValue.getValue()).doubleValue());
        pairProcessController.setWavesSecondaryPercent(((Number) spinnerBuyWavesSecondPercent.getValue()).doubleValue());
        pairProcessController.setWavesIncKoef(((Number) spinnerBuyWavesIncKoef.getValue()).doubleValue());
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonRun = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();
        textFieldTradePairs = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
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
        listRating = new javax.swing.JList<>();
        jLabel8 = new javax.swing.JLabel();
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
        textFieldApiSecret = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        textFieldApiKey = new javax.swing.JTextField();
        textFieldBaseCoin = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        spinnerTestAdd = new javax.swing.JSpinner();
        checkBoxTestAdd = new javax.swing.JCheckBox();
        textFieldTestCoinAdd = new javax.swing.JTextField();
        jPanel10 = new javax.swing.JPanel();
        checkBoxStopLoss = new javax.swing.JCheckBox();
        spinnerStopLoss = new javax.swing.JSpinner();
        spinnerStopGain = new javax.swing.JSpinner();
        checkBoxStopGain = new javax.swing.JCheckBox();
        checkBoxLowHold = new javax.swing.JCheckBox();
        spinnerBuyPercent = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        comboBoxBarsInterval = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        comboBoxBarsCount = new javax.swing.JComboBox<>();
        jLabel14 = new javax.swing.JLabel();
        spinnerBuyMainValue = new javax.swing.JSpinner();
        jLabel44 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        checkBoxCheckOtherStrategies = new javax.swing.JCheckBox();
        ComboBoxMainStrategy = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        listBoxAutoStrategies = new javax.swing.JList<>();
        jLabel23 = new javax.swing.JLabel();
        checkBoxWalkForward = new javax.swing.JCheckBox();
        checkBoxAutoStrategyParams = new javax.swing.JCheckBox();
        checkBoxPyramiding = new javax.swing.JCheckBox();
        spinnerPyramidingMaxC = new javax.swing.JSpinner();
        labelAfterPyramidingMax = new javax.swing.JLabel();
        checkBoxDowntrendNoAuto = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        checkboxAutoOrder = new javax.swing.JCheckBox();
        checkBoxAutoAnalyzer = new javax.swing.JCheckBox();
        spinnerScanRatingDelayTime = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        spinnerScanRatingUpdateTime = new javax.swing.JSpinner();
        checkboxAutoFastorder = new javax.swing.JCheckBox();
        jLabel22 = new javax.swing.JLabel();
        spinnerRatingMinForOrder = new javax.swing.JSpinner();
        spinnerPricesUpdateDelay = new javax.swing.JSpinner();
        jLabel32 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        spinnerScanTrendUpdateTime = new javax.swing.JSpinner();
        jLabel40 = new javax.swing.JLabel();
        spinnerRatingMinBaseVolume = new javax.swing.JSpinner();
        jLabel47 = new javax.swing.JLabel();
        spinnerScanRanksUpdateTime = new javax.swing.JSpinner();
        jPanel12 = new javax.swing.JPanel();
        spinnerRatingMaxOrders = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        spinnerRatingMaxOrderWait = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        textFieldAPIID = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        textFieldAPIHash = new javax.swing.JTextField();
        buttonTlgConnect = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        textFieldPhone = new javax.swing.JTextField();
        checkboxAutoSignalOrder = new javax.swing.JCheckBox();
        checkboxAutoSignalFastorder = new javax.swing.JCheckBox();
        jLabel24 = new javax.swing.JLabel();
        spinnerSignalRatingMinForOrder = new javax.swing.JSpinner();
        jLabel25 = new javax.swing.JLabel();
        spinnerSignalPreloadCount = new javax.swing.JSpinner();
        checkBoxUseSignals = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        checkBoxWavesUse = new javax.swing.JCheckBox();
        spinnerBuyWavesFirstPercent = new javax.swing.JSpinner();
        jLabel48 = new javax.swing.JLabel();
        spinnerBuyWavesFirstMainValue = new javax.swing.JSpinner();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        spinnerBuyWavesSecondPercent = new javax.swing.JSpinner();
        jLabel51 = new javax.swing.JLabel();
        spinnerBuyWavesIncKoef = new javax.swing.JSpinner();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        spinnerWavesInitialProfitPercent = new javax.swing.JSpinner();
        spinnerWavesHalfProfitOrdersCnt = new javax.swing.JSpinner();
        jLabel54 = new javax.swing.JLabel();
        spinnerWavesMinProfitPercent = new javax.swing.JSpinner();
        jLabel55 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        checkBoxUseCycles = new javax.swing.JCheckBox();
        textFieldRestrictedCoins = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        spinnerCycleDelay = new javax.swing.JSpinner();
        jLabel29 = new javax.swing.JLabel();
        spinnerCyclePercent = new javax.swing.JSpinner();
        jLabel30 = new javax.swing.JLabel();
        spinnerCycleAbortTime = new javax.swing.JSpinner();
        jLabel31 = new javax.swing.JLabel();
        textFieldRequiredCoins = new javax.swing.JTextField();
        jLabel33 = new javax.swing.JLabel();
        spinnerCycleMinProfitPercent = new javax.swing.JSpinner();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        spinnerCycleMaxCoinRank = new javax.swing.JSpinner();
        spinnerCycleMinPairRating = new javax.swing.JSpinner();
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        spinnerCycleMinBaseVolume = new javax.swing.JSpinner();
        textFieldCycleMainCoins = new javax.swing.JTextField();
        jLabel39 = new javax.swing.JLabel();
        spinnerCycleSwitchTime = new javax.swing.JSpinner();
        jLabel41 = new javax.swing.JLabel();
        spinnerCycleFirstAbortTime = new javax.swing.JSpinner();
        jLabel42 = new javax.swing.JLabel();
        spinnerCycleMaxSwitches = new javax.swing.JSpinner();
        jLabel43 = new javax.swing.JLabel();
        spinnerCycleMaxEnterCount = new javax.swing.JSpinner();
        checkBoxCycleDepthCheck = new javax.swing.JCheckBox();
        jPanel8 = new javax.swing.JPanel();
        buttonNNBTrain = new javax.swing.JButton();
        buttonNNBAdd = new javax.swing.JButton();
        buttonNNCTrain = new javax.swing.JButton();
        buttonNNCAdd = new javax.swing.JButton();
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
        buttonRemove = new javax.swing.JButton();
        labelAccountCost = new javax.swing.JLabel();
        ButtonShowLog = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        labelDownTrend = new javax.swing.JLabel();
        labelUpTrend = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        labelMCVolume = new javax.swing.JLabel();
        labelMCCap = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        labelMCBTCDominance = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

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

        buttonBuy.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonBuy.setText("Buy");
        buttonBuy.setToolTipText("Buy selected coin");
        buttonBuy.setEnabled(false);
        buttonBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBuyActionPerformed(evt);
            }
        });

        buttonSell.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonSell.setText("Sell");
        buttonSell.setToolTipText("Sell selected coin");
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

        buttonUpdate.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonUpdate.setText("Update");
        buttonUpdate.setToolTipText("Update balances");
        buttonUpdate.setEnabled(false);
        buttonUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpdateActionPerformed(evt);
            }
        });

        buttonCancelLimit.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonCancelLimit.setText("Cancel LO");
        buttonCancelLimit.setToolTipText("Cancel limit order");
        buttonCancelLimit.setEnabled(false);
        buttonCancelLimit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelLimitActionPerformed(evt);
            }
        });

        buttonShowPlot.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonShowPlot.setText("Show plot");
        buttonShowPlot.setToolTipText("Show plot");
        buttonShowPlot.setEnabled(false);
        buttonShowPlot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonShowPlotActionPerformed(evt);
            }
        });

        listRating.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listRating.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listRatingMouseClicked(evt);
            }
        });
        jScrollPane5.setViewportView(listRating);

        jLabel8.setText("Coins rating");

        jLabel10.setText("Trading pairs:");

        jLabel11.setText("Coins:");

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        spinnerUpdateDelay.setValue(10);

        jLabel5.setText("Pairs update delay (sec):");

        checkboxTestMode.setSelected(true);
        checkboxTestMode.setText("Test mode");
        checkboxTestMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxTestModeActionPerformed(evt);
            }
        });

        checkBoxLimitedOrders.setText("Limited orders");
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
        comboBoxLimitedMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxLimitedModeActionPerformed(evt);
            }
        });

        jLabel9.setText("Api Secret / Api Key:");

        textFieldBaseCoin.setText("btc");

        jLabel28.setText("Base coin:");

        spinnerTestAdd.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.1f), Float.valueOf(1.0E-4f), Float.valueOf(10000.0f), Float.valueOf(0.001f)));
        spinnerTestAdd.setValue(1.00);

        checkBoxTestAdd.setText("Add");

        textFieldTestCoinAdd.setText("btc");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxLimitedOrders)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxLimitedMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkboxTestMode)
                        .addGap(20, 20, 20)
                        .addComponent(checkBoxTestAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerTestAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldTestCoinAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(checkBoxBuyStopLimited)
                            .addComponent(checkBoxSellStopLimited))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerSellStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerBuyStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 211, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel9)
                        .addComponent(textFieldApiSecret, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(textFieldApiKey, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel28)
                        .addComponent(textFieldBaseCoin, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldApiSecret, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldApiKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(34, 34, 34)
                        .addComponent(jLabel28)
                        .addGap(3, 3, 3)
                        .addComponent(textFieldBaseCoin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(spinnerUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkboxTestMode)
                            .addComponent(spinnerTestAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxTestAdd)
                            .addComponent(textFieldTestCoinAdd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxLimitedMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxLimitedOrders))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerBuyStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13)
                            .addComponent(checkBoxBuyStopLimited))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerSellStopLimited, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12)
                            .addComponent(checkBoxSellStopLimited))))
                .addContainerGap(68, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Main", jPanel2);

        jPanel10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

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

        checkBoxStopGain.setText("Stop Gain percent:");
        checkBoxStopGain.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxStopGainStateChanged(evt);
            }
        });

        checkBoxLowHold.setSelected(true);
        checkBoxLowHold.setText("Hold on low profit");
        checkBoxLowHold.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxLowHoldStateChanged(evt);
            }
        });

        spinnerBuyPercent.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spinnerBuyPercent.setValue(50);

        jLabel3.setText("Order quote %:");

        comboBoxBarsInterval.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1m", "5m", "15m", "30m", "1h", "2h" }));

        jLabel2.setText("Bars interval:");

        comboBoxBarsCount.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "500", "1000", "1500", "2000", "3000", "4000", "5000" }));

        jLabel14.setText("Bars count:");

        spinnerBuyMainValue.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(1000000.0f), Float.valueOf(0.001f)));
        spinnerBuyMainValue.setValue(0);

        jLabel44.setText("Order main value:");

        jLabel6.setText("+");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(spinnerBuyPercent, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel44)
                            .addComponent(spinnerBuyMainValue, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(comboBoxBarsInterval, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(comboBoxBarsCount, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel14)))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(checkBoxStopLoss)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerStopLoss, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkBoxLowHold)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(checkBoxStopGain)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerStopGain, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(369, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel14)
                    .addComponent(jLabel3)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboBoxBarsInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboBoxBarsCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerBuyPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerBuyMainValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxStopLoss)
                    .addComponent(spinnerStopLoss, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxStopGain)
                    .addComponent(spinnerStopGain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkBoxLowHold)
                .addContainerGap(104, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Orders", jPanel10);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        checkBoxCheckOtherStrategies.setText("Test all other strategies");

        ComboBoxMainStrategy.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Auto" }));

        jLabel4.setText("Main strategy:");

        jScrollPane7.setViewportView(listBoxAutoStrategies);

        jLabel23.setText("Strategies for auto select:");

        checkBoxWalkForward.setText("WalkForward");
        checkBoxWalkForward.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxWalkForwardActionPerformed(evt);
            }
        });

        checkBoxAutoStrategyParams.setText("Auto pick strategy params");
        checkBoxAutoStrategyParams.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAutoStrategyParamsActionPerformed(evt);
            }
        });

        checkBoxPyramiding.setText("Pyramiding");
        checkBoxPyramiding.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxPyramidingActionPerformed(evt);
            }
        });

        spinnerPyramidingMaxC.setModel(new javax.swing.SpinnerNumberModel(0, 0, 50, 1));
        spinnerPyramidingMaxC.setEnabled(false);
        spinnerPyramidingMaxC.setValue(1);

        labelAfterPyramidingMax.setText("max. contracts");
        labelAfterPyramidingMax.setEnabled(false);

        checkBoxDowntrendNoAuto.setText("No auto orders on downtrend");
        checkBoxDowntrendNoAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxDowntrendNoAutoActionPerformed(evt);
            }
        });

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
                        .addComponent(ComboBoxMainStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(checkBoxCheckOtherStrategies)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxAutoStrategyParams))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel23)
                                .addGap(108, 108, 108)
                                .addComponent(checkBoxWalkForward)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(checkBoxPyramiding)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerPyramidingMaxC, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelAfterPyramidingMax))
                            .addComponent(checkBoxDowntrendNoAuto))))
                .addGap(162, 162, 162))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel4)
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ComboBoxMainStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(checkBoxCheckOtherStrategies)
                        .addComponent(checkBoxAutoStrategyParams)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(checkBoxWalkForward))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerPyramidingMaxC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelAfterPyramidingMax)
                            .addComponent(checkBoxPyramiding))
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxDowntrendNoAuto)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane2.addTab("Strategies", jPanel1);

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        checkboxAutoOrder.setText("Auto order");
        checkboxAutoOrder.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkboxAutoOrderStateChanged(evt);
            }
        });
        checkboxAutoOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxAutoOrderActionPerformed(evt);
            }
        });

        checkBoxAutoAnalyzer.setText("Deep analysis");
        checkBoxAutoAnalyzer.setToolTipText("Check volume, strategies, volatility. Create rating");

        spinnerScanRatingDelayTime.setValue(2);
        spinnerScanRatingDelayTime.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScanRatingDelayTimeStateChanged(evt);
            }
        });

        jLabel15.setText("Next coin check delay (sec):");

        jLabel16.setText("Update time (sec):");

        spinnerScanRatingUpdateTime.setValue(10);

        checkboxAutoFastorder.setText("Fast auto order");
        checkboxAutoFastorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxAutoFastorderActionPerformed(evt);
            }
        });

        jLabel22.setText("Min rating for order:");

        spinnerRatingMinForOrder.setValue(5);

        spinnerPricesUpdateDelay.setValue(30);

        jLabel32.setText("Prices update delay (sec):");

        jLabel38.setText("Update trend time (sec):");

        spinnerScanTrendUpdateTime.setValue(450);

        jLabel40.setText("Min base coin volume:");

        spinnerRatingMinBaseVolume.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(20.0f), Float.valueOf(1.0E-4f), Float.valueOf(1.0E9f), Float.valueOf(0.001f)));
        spinnerRatingMinBaseVolume.setValue(0.001);

        jLabel47.setText("Update ranks time (sec):");

        spinnerScanRanksUpdateTime.setValue(480);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(checkBoxAutoAnalyzer, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(64, 64, 64)
                                .addComponent(jLabel16))
                            .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel38, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel47, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(5, 5, 5)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(spinnerScanRatingUpdateTime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE)
                            .addComponent(spinnerScanTrendUpdateTime, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(spinnerScanRatingDelayTime)
                            .addComponent(spinnerScanRanksUpdateTime))))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(jLabel32)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(spinnerPricesUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addGap(20, 20, 20)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel22)
                                .addComponent(jLabel40))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(spinnerRatingMinForOrder, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(spinnerRatingMinBaseVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(checkboxAutoOrder)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(checkboxAutoFastorder)))
                .addGap(0, 267, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxAutoAnalyzer)
                    .addComponent(jLabel32)
                    .addComponent(spinnerPricesUpdateDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerScanRatingDelayTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel16)
                            .addComponent(spinnerScanRatingUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerScanTrendUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel38))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel47)
                            .addComponent(spinnerScanRanksUpdateTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkboxAutoOrder)
                            .addComponent(checkboxAutoFastorder))
                        .addGap(17, 17, 17)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(spinnerRatingMinForOrder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerRatingMinBaseVolume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel40))))
                .addContainerGap(115, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Coins rating", jPanel4);

        jPanel12.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        spinnerRatingMaxOrders.setValue(3);

        jLabel19.setText("Max orders:");

        jLabel21.setText("Max order wait (sec):");

        spinnerRatingMaxOrderWait.setValue(600);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel21)
                    .addComponent(jLabel19))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(spinnerRatingMaxOrders)
                    .addComponent(spinnerRatingMaxOrderWait, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(533, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerRatingMaxOrders, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(spinnerRatingMaxOrderWait, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(198, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Auto orders", jPanel12);

        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel17.setText("API ID:");

        jLabel18.setText("API Hash:");

        buttonTlgConnect.setText("Test");
        buttonTlgConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTlgConnectActionPerformed(evt);
            }
        });

        jLabel20.setText("Phone:");

        checkboxAutoSignalOrder.setText("Auto order");
        checkboxAutoSignalOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxAutoSignalOrderActionPerformed(evt);
            }
        });

        checkboxAutoSignalFastorder.setText("Fast auto order");
        checkboxAutoSignalFastorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkboxAutoSignalFastorderActionPerformed(evt);
            }
        });

        jLabel24.setText("Min rating for order:");

        spinnerSignalRatingMinForOrder.setValue(5);

        jLabel25.setText("Preload signals:");

        spinnerSignalPreloadCount.setValue(200);

        checkBoxUseSignals.setText("Use signals");
        checkBoxUseSignals.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUseSignalsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel24)
                            .addComponent(jLabel25))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spinnerSignalPreloadCount, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerSignalRatingMinForOrder, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(checkBoxUseSignals)
                        .addGap(18, 18, 18)
                        .addComponent(checkboxAutoSignalOrder)
                        .addGap(18, 18, 18)
                        .addComponent(checkboxAutoSignalFastorder))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(textFieldAPIID, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel18)
                            .addComponent(textFieldAPIHash, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(textFieldPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonTlgConnect))
                            .addComponent(jLabel20))))
                .addContainerGap(262, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jLabel18)
                    .addComponent(jLabel20))
                .addGap(6, 6, 6)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textFieldAPIID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldAPIHash, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(textFieldPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonTlgConnect))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxUseSignals)
                    .addComponent(checkboxAutoSignalOrder)
                    .addComponent(checkboxAutoSignalFastorder))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerSignalPreloadCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(spinnerSignalRatingMinForOrder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(107, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Signals", jPanel5);

        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        checkBoxWavesUse.setText("Use waves for all pair trades");

        spinnerBuyWavesFirstPercent.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(100.0f), Float.valueOf(0.1f)));
        spinnerBuyWavesFirstPercent.setValue(10);

        jLabel48.setText("First order quote %:");

        spinnerBuyWavesFirstMainValue.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(1000000.0f), Float.valueOf(0.0001f)));
        spinnerBuyWavesFirstMainValue.setEditor(new javax.swing.JSpinner.NumberEditor(spinnerBuyWavesFirstMainValue, "0.0000"));
        spinnerBuyWavesFirstMainValue.setValue(0);

        jLabel49.setText("First order main value:");

        jLabel50.setText("+");

        spinnerBuyWavesSecondPercent.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(25.0f), Float.valueOf(1.0E-4f), Float.valueOf(100.0f), Float.valueOf(0.001f)));
        spinnerBuyWavesSecondPercent.setValue(25);

        jLabel51.setText("Second wave %:");

        spinnerBuyWavesIncKoef.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(59.0f), Float.valueOf(1.0E-4f), Float.valueOf(10000.0f), Float.valueOf(0.001f)));
        spinnerBuyWavesIncKoef.setValue(59);

        jLabel52.setText("Inc koef:");

        jLabel53.setText("Initial profit %:");

        spinnerWavesInitialProfitPercent.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(8.0f), Float.valueOf(1.0E-4f), Float.valueOf(100.0f), Float.valueOf(0.001f)));
        spinnerWavesInitialProfitPercent.setValue(8);

        spinnerWavesHalfProfitOrdersCnt.setModel(new javax.swing.SpinnerNumberModel(11, 1, 1000, 1));
        spinnerWavesHalfProfitOrdersCnt.setValue(11);

        jLabel54.setText("Half profit orders count:");

        spinnerWavesMinProfitPercent.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.75f), Float.valueOf(0.0f), Float.valueOf(100.0f), Float.valueOf(0.001f)));
        spinnerWavesMinProfitPercent.setValue(0.75);

        jLabel55.setText("Min profit %:");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel52)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerBuyWavesIncKoef, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel51)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerBuyWavesSecondPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(32, 32, 32)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel54)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerWavesHalfProfitOrdersCnt, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel53)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerWavesInitialProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel55)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerWavesMinProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(spinnerBuyWavesFirstPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel50))
                            .addComponent(jLabel48))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel49)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(spinnerBuyWavesFirstMainValue, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(72, 72, 72)
                                .addComponent(checkBoxWavesUse)))))
                .addContainerGap(279, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel48)
                    .addComponent(jLabel49))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerBuyWavesFirstPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerBuyWavesFirstMainValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel50)
                    .addComponent(checkBoxWavesUse))
                .addGap(25, 25, 25)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerWavesMinProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel55))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerWavesInitialProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel53))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerWavesHalfProfitOrdersCnt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel54)))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerBuyWavesSecondPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel51))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spinnerBuyWavesIncKoef, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel52))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Waves", jPanel7);

        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        checkBoxUseCycles.setText("Use cycles auto orders");
        checkBoxUseCycles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUseCyclesActionPerformed(evt);
            }
        });

        textFieldRestrictedCoins.setText("tnt,xrp");

        jLabel27.setText("Restricted coins:");

        spinnerCycleDelay.setValue(90);

        jLabel29.setText("Update delay (sec):");

        spinnerCyclePercent.setValue(25);

        jLabel30.setText("Cycle coin percent:");

        spinnerCycleAbortTime.setValue(43200);

        jLabel31.setText("Abort first/all time (sec):");

        textFieldRequiredCoins.setText("btc,eth,bnb,usdt");

        jLabel33.setText("Required coins:");

        spinnerCycleMinProfitPercent.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.95f), Float.valueOf(0.01f), Float.valueOf(20.0f), Float.valueOf(0.01f)));

        jLabel34.setText("Min. profit percent:");

        jLabel35.setText("Max coin rank:");

        spinnerCycleMaxCoinRank.setValue(250);

        spinnerCycleMinPairRating.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.95f), Float.valueOf(0.01f), Float.valueOf(100.0f), Float.valueOf(0.01f)));
        spinnerCycleMinPairRating.setValue(2.00);

        jLabel36.setText("Min pair rating:");

        jLabel37.setText("Min base coin pair volume (1hr):");

        spinnerCycleMinBaseVolume.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(20.0f), Float.valueOf(1.0E-4f), Float.valueOf(1.0E9f), Float.valueOf(0.001f)));
        spinnerCycleMinBaseVolume.setValue(20.00);

        textFieldCycleMainCoins.setText("btc,eth,usdt,eos");

        jLabel39.setText("Main coins:");

        spinnerCycleSwitchTime.setValue(1800);

        jLabel41.setText("Switch time (sec):");

        spinnerCycleFirstAbortTime.setValue(1200);

        jLabel42.setText("Max switches:");

        spinnerCycleMaxSwitches.setValue(3);

        jLabel43.setText("Max concurrent cycles:");

        spinnerCycleMaxEnterCount.setValue(2);

        checkBoxCycleDepthCheck.setText("Depth checking");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel29)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCycleDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel31)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCycleFirstAbortTime, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel41)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCycleSwitchTime, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel42)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCycleMaxSwitches, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel30)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCyclePercent, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                .addComponent(spinnerCycleAbortTime, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel35)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCycleMaxCoinRank, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                        .addComponent(jLabel34)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerCycleMinProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                        .addComponent(jLabel36)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerCycleMinPairRating, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                        .addComponent(jLabel37)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerCycleMinBaseVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                        .addComponent(jLabel43)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerCycleMaxEnterCount, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxUseCycles)
                            .addComponent(checkBoxCycleDepthCheck))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel39)
                            .addComponent(textFieldCycleMainCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel33)
                            .addComponent(textFieldRequiredCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel27)
                            .addComponent(textFieldRestrictedCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel39)
                            .addComponent(checkBoxUseCycles))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(textFieldCycleMainCoins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxCycleDepthCheck)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldRequiredCoins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldRestrictedCoins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(spinnerCycleDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34)
                    .addComponent(spinnerCycleMinProfitPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel31)
                            .addComponent(spinnerCycleFirstAbortTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerCycleAbortTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel41)
                            .addComponent(spinnerCycleSwitchTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel35)
                            .addComponent(spinnerCycleMaxCoinRank, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel36)
                            .addComponent(spinnerCycleMinPairRating, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spinnerCycleMaxSwitches, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spinnerCycleMinBaseVolume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel37)
                        .addComponent(jLabel42)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spinnerCyclePercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel30))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel43)
                        .addComponent(spinnerCycleMaxEnterCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Cycles", jPanel6);

        jPanel8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        buttonNNBTrain.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonNNBTrain.setText("NN B Train");
        buttonNNBTrain.setToolTipText("Neural network Base Train");
        buttonNNBTrain.setEnabled(false);
        buttonNNBTrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNNBTrainActionPerformed(evt);
            }
        });

        buttonNNBAdd.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonNNBAdd.setText("NN B Add");
        buttonNNBAdd.setToolTipText("Neural network Base Add to dataset");
        buttonNNBAdd.setEnabled(false);
        buttonNNBAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNNBAddActionPerformed(evt);
            }
        });

        buttonNNCTrain.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonNNCTrain.setText("NN C Train");
        buttonNNCTrain.setToolTipText("Neural network Coin Train");
        buttonNNCTrain.setEnabled(false);
        buttonNNCTrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNNCTrainActionPerformed(evt);
            }
        });

        buttonNNCAdd.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonNNCAdd.setText("NN C Add");
        buttonNNCAdd.setToolTipText("Neural network Coin add to dataset");
        buttonNNCAdd.setEnabled(false);
        buttonNNCAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNNCAddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonNNBTrain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonNNBAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonNNCTrain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonNNCAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(197, 197, 197))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonNNBTrain)
                    .addComponent(buttonNNBAdd)
                    .addComponent(buttonNNCTrain)
                    .addComponent(buttonNNCAdd))
                .addContainerGap(225, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("NN", jPanel8);

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Modifiers:\nBuy for best price, then quit: -\nSell all free base coins for this pair, then quit: +\nSell all free and all limit orders for this pair, then quit: ++\nForce waves mode: ~\nQuick waves limit mode: `\nInitial short mode: _");
        jTextArea1.setWrapStyleWord(true);
        jScrollPane6.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Tips", jPanel3);

        ButtonStatistics.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        ButtonStatistics.setText("Statistics");
        ButtonStatistics.setToolTipText("Show coin strategy statistics");
        ButtonStatistics.setEnabled(false);
        ButtonStatistics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonStatisticsActionPerformed(evt);
            }
        });

        buttonWebBrowserOpen.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonWebBrowserOpen.setText("Binance");
        buttonWebBrowserOpen.setToolTipText("Open coin page");
        buttonWebBrowserOpen.setEnabled(false);
        buttonWebBrowserOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonWebBrowserOpenActionPerformed(evt);
            }
        });

        comboBoxRatingSortby.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Rank", "Market cap", "Hour Volume in Base Currency", "Day Volume in Base Currency", "% from prog start", "% last hour", "% 24hr", "Events count", "Last event anno date", "Volatility", "Strategies to enter value", "Strategies to exit value", "Strategies value", "Signals rating", "Calculated rating" }));
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

        buttonRemove.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        buttonRemove.setText("Remove");
        buttonRemove.setToolTipText("Remove selected coin");
        buttonRemove.setEnabled(false);
        buttonRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveActionPerformed(evt);
            }
        });

        labelAccountCost.setText("Account cost: Unknown");

        ButtonShowLog.setFont(new java.awt.Font("Tahoma", 0, 8)); // NOI18N
        ButtonShowLog.setText("Show log");
        ButtonShowLog.setToolTipText("");
        ButtonShowLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonShowLogActionPerformed(evt);
            }
        });

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true), "Global trend:", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP));

        labelDownTrend.setForeground(new java.awt.Color(255, 0, 102));
        labelDownTrend.setText("DOWN: 0%");

        labelUpTrend.setForeground(new java.awt.Color(0, 204, 51));
        labelUpTrend.setText("UP: 0%");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelDownTrend)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(labelUpTrend)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDownTrend)
                    .addComponent(labelUpTrend))
                .addContainerGap())
        );

        jLabel7.setText("Volume:");

        jLabel45.setText("Marketcap:");

        labelMCVolume.setText("Unknown");

        labelMCCap.setText("Unknown");

        jLabel46.setText("BTC perc:");

        labelMCBTCDominance.setText("Unknown");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel46, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel45, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelMCBTCDominance, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                    .addComponent(labelMCVolume, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(labelMCCap, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(labelMCVolume))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(labelMCCap))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel46)
                    .addComponent(labelMCBTCDominance))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonRatingStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(buttonRatingStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonRatingCheck))
                    .addComponent(progressBarRatingAnalPercent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel8)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(comboBoxRatingSortby, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxRatingSort, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelAccountCost))
                            .addComponent(jScrollPane3)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(textFieldTradePairs)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSetPairs))
                    .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonRun, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonStop, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPause, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonBuy, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonWebBrowserOpen))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSell, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancelLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemove, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ButtonShowLog, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buttonShowPlot, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ButtonStatistics, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))))
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
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonRun)
                            .addComponent(buttonStop)
                            .addComponent(buttonPause))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(labelAccountCost)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                            .addComponent(jScrollPane3)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboBoxRatingSortby, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBoxRatingSort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(progressBarRatingAnalPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonRatingCheck)
                            .addComponent(buttonRatingStop)
                            .addComponent(buttonRatingStart)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonBuy)
                            .addComponent(buttonSell)
                            .addComponent(buttonUpdate)
                            .addComponent(buttonCancelLimit)
                            .addComponent(buttonRemove))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonWebBrowserOpen))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonShowPlot)
                            .addComponent(ButtonStatistics))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ButtonShowLog)))
                .addGap(19, 19, 19))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        log("Starting...\n", true, true);
        is_paused = false;
        buttonRun.setEnabled(false);
        try {
            initAPI();
            OrdersController.getInstance().setAutoPickStrategyParams(checkBoxAutoStrategyParams.isSelected());
            OrdersController.getInstance().setAutoWalkForward(checkBoxWalkForward.isSelected());
            OrdersController.getInstance().setAutoStrategies(listBoxAutoStrategies.getSelectedValuesList());
            OrdersController.getInstance().setTestMode(checkboxTestMode.isSelected());
            OrdersController.getInstance().setLimitedOrders(checkBoxLimitedOrders.isSelected());
            OrdersController.getInstance().setLimitedOrderMode(comboBoxLimitedMode.getSelectedIndex() == 0 ? OrdersController.LimitedOrderMode.LOMODE_SELL : OrdersController.LimitedOrderMode.LOMODE_SELLANDBUY);
            OrdersController.getInstance().setStopGainPercent(checkBoxStopGain.isSelected() ? BigDecimal.valueOf(((Number) spinnerStopGain.getValue()).longValue()) : null);
            OrdersController.getInstance().setStopLossPercent(checkBoxStopLoss.isSelected() ? BigDecimal.valueOf(((Number) spinnerStopLoss.getValue()).longValue()) : null);
            OrdersController.getInstance().setLowHold(checkBoxLowHold.isSelected());
            OrdersController.getInstance().setDelayTime(((Number) spinnerUpdateDelay.getValue()).longValue());
            BalanceController.getInstance().setDelayTime(240);
            BalanceController.getInstance().setTestMode(checkboxTestMode.isSelected());
            BalanceController.getInstance().setStartCoinBalance(checkBoxTestAdd.isSelected() ? (textFieldTestCoinAdd.getText()) : null, ((Number) spinnerTestAdd.getValue()).doubleValue());
            
            BalanceController.getInstance().start();
            OrdersController.getInstance().start();
            
            log("", true);
            setPairParams();
            pairProcessController.initBasePairs(textFieldTradePairs.getText());
            checkboxTestMode.setEnabled(false);
            buttonStop.setEnabled(true);
            buttonPause.setEnabled(true);
            buttonBuy.setEnabled(true);
            buttonSell.setEnabled(true);
            buttonSetPairs.setEnabled(true);
            buttonUpdate.setEnabled(true);
            buttonCancelLimit.setEnabled(checkBoxLimitedOrders.isSelected());
            buttonShowPlot.setEnabled(true);
            ButtonStatistics.setEnabled(true);
            buttonWebBrowserOpen.setEnabled(true);
            buttonNNBAdd.setEnabled(true);
            buttonNNCAdd.setEnabled(true);
            buttonNNBTrain.setEnabled(true);
            buttonNNCTrain.setEnabled(true);
            buttonRemove.setEnabled(true);
            labelAccountCost.setEnabled(!checkboxTestMode.isSelected());
            checkBoxTestAdd.setEnabled(false);
            spinnerTestAdd.setEnabled(false);
            textFieldTestCoinAdd.setEnabled(false);

        } catch (Exception exx) {
            exx.printStackTrace(System.out);
            log("");
            log("EXCEPTION - " + exx.getClass() + ": " + exx.getLocalizedMessage(), true, true);
            buttonRun.setEnabled(true);
        }
    }//GEN-LAST:event_buttonRunActionPerformed

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStopActionPerformed
        is_paused = false;
        buttonStop.setEnabled(false);
        buttonPause.setEnabled(false);
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
        buttonNNBAdd.setEnabled(false);
        buttonNNCAdd.setEnabled(false);
        buttonNNBTrain.setEnabled(false);
        buttonNNCTrain.setEnabled(false);
        buttonRemove.setEnabled(false);
        labelAccountCost.setEnabled(true);
        checkBoxTestAdd.setEnabled(checkboxTestMode.isSelected());
        spinnerTestAdd.setEnabled(checkboxTestMode.isSelected());
        textFieldTestCoinAdd.setEnabled(checkboxTestMode.isSelected());
        pairProcessController.stopPairs();
        OrdersController.getInstance().doStop();
        BalanceController.getInstance().doStop();
        if (coinRatingController.isStop()) CoinInfoAggregator.getInstance().doStop();
    }//GEN-LAST:event_buttonStopActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        is_paused = !is_paused;
        buttonStop.setEnabled(true);
        buttonPause.setEnabled(true);
        buttonRun.setEnabled(false);
        buttonBuy.setEnabled(true);
        buttonSell.setEnabled(true);
        checkboxTestMode.setEnabled(false);
        buttonSetPairs.setEnabled(true);
        buttonUpdate.setEnabled(true);
        buttonCancelLimit.setEnabled(checkBoxLimitedOrders.isSelected());
        buttonShowPlot.setEnabled(true);
        log(is_paused ? "Paused..." : "Unpaused");
        pairProcessController.pausePairs(is_paused);
        coinRatingController.doSetPaused(is_paused);
        ButtonStatistics.setEnabled(true);
        buttonWebBrowserOpen.setEnabled(true);
        buttonNNBAdd.setEnabled(true);
        buttonNNCAdd.setEnabled(true);
        buttonNNBTrain.setEnabled(true);
        buttonNNCTrain.setEnabled(true);
        buttonRemove.setEnabled(true);
        labelAccountCost.setEnabled(!checkboxTestMode.isSelected());
        checkBoxTestAdd.setEnabled(false);
        spinnerTestAdd.setEnabled(false);
        textFieldTestCoinAdd.setEnabled(false);
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBuyActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "BUY");
    }//GEN-LAST:event_buttonBuyActionPerformed

    private void buttonSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSellActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "SELL");
    }//GEN-LAST:event_buttonSellActionPerformed

    private void buttonSetPairsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetPairsActionPerformed
        OrdersController.getInstance().setAutoPickStrategyParams(checkBoxAutoStrategyParams.isSelected());
        OrdersController.getInstance().setAutoWalkForward(checkBoxWalkForward.isSelected());
        OrdersController.getInstance().setAutoStrategies(listBoxAutoStrategies.getSelectedValuesList());
        OrdersController.getInstance().setLimitedOrders(checkBoxLimitedOrders.isSelected());
        OrdersController.getInstance().setStopGainPercent(checkBoxStopGain.isSelected() ? BigDecimal.valueOf(((Number) spinnerStopGain.getValue()).longValue()) : null);
        OrdersController.getInstance().setStopLossPercent(checkBoxStopLoss.isSelected() ? BigDecimal.valueOf(((Number) spinnerStopLoss.getValue()).longValue()) : null);
        OrdersController.getInstance().setLowHold(checkBoxLowHold.isSelected());
        setPairParams();
        pairProcessController.initBasePairs(textFieldTradePairs.getText());
    }//GEN-LAST:event_buttonSetPairsActionPerformed

    private void buttonUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpdateActionPerformed
        BalanceController.getInstance().updateAllBalances();
    }//GEN-LAST:event_buttonUpdateActionPerformed

    private void buttonCancelLimitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelLimitActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "CANCEL");
    }//GEN-LAST:event_buttonCancelLimitActionPerformed

    private void buttonShowPlotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShowPlotActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "PLOT");
    }//GEN-LAST:event_buttonShowPlotActionPerformed

    private void checkBoxLimitedOrdersStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxLimitedOrdersStateChanged
        OrdersController.getInstance().setLimitedOrders(checkBoxLimitedOrders.isSelected());
    }//GEN-LAST:event_checkBoxLimitedOrdersStateChanged

    private void listRatingMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listRatingMouseClicked
        if (evt.getClickCount() == 2) {
            String content = listRating.getSelectedValue();
            if (content != null && !content.isEmpty()) {
                String[] parts = content.split(":");
                if (parts.length > 0) {
                    String newtext = "";
                    if (parts[0].indexOf(')') > 0) {
                        parts = parts[0].split(" ");
                        if (parts.length > 1) {
                            newtext = parts[1].toLowerCase();
                        }
                    } else {
                        newtext = parts[0].toLowerCase();
                    }
                    if (!newtext.isEmpty()) {
                        if (!textFieldTradePairs.getText().isEmpty()) {
                            newtext = textFieldTradePairs.getText() + "," + newtext;
                        }
                        textFieldTradePairs.setText(newtext);
                    }
                }
            }
        }
    }//GEN-LAST:event_listRatingMouseClicked

    private void listCurrenciesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listCurrenciesMouseClicked
        if (evt.getClickCount() == 2) {
            buttonShowPlotActionPerformed(null);
        }
    }//GEN-LAST:event_listCurrenciesMouseClicked

    private void checkBoxLowHoldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxLowHoldStateChanged
        coinRatingController.setLowHold(checkBoxLowHold.isSelected());
    }//GEN-LAST:event_checkBoxLowHoldStateChanged

    private void checkboxAutoOrderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkboxAutoOrderStateChanged
        coinRatingController.setAutoOrder(checkboxAutoOrder.isSelected());
    }//GEN-LAST:event_checkboxAutoOrderStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        config.Save();
        if (coinRatingController != null && coinRatingController.isAlive()) {
            if (pairProcessController != null) {
                pairProcessController.pausePairs(true);
            }
            coinRatingController.doStop();
            coinRatingController.getSignalOrderController().doStop();
            int i = 0;
            while (++i < 20 && (coinRatingController.isAlive() || coinRatingController.getSignalOrderController().isAlive())) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(mainApplication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
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
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "STATISTICS");
    }//GEN-LAST:event_ButtonStatisticsActionPerformed

    private void buttonWebBrowserOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonWebBrowserOpenActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "BROWSER");
    }//GEN-LAST:event_buttonWebBrowserOpenActionPerformed

    private void checkBoxLimitedOrdersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxLimitedOrdersActionPerformed
        comboBoxLimitedMode.setEnabled(checkBoxLimitedOrders.isSelected());
    }//GEN-LAST:event_checkBoxLimitedOrdersActionPerformed

    private void comboBoxLimitedModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxLimitedModeActionPerformed
        OrdersController.getInstance().setLimitedOrderMode(comboBoxLimitedMode.getSelectedIndex() == 0 ? OrdersController.LimitedOrderMode.LOMODE_SELL : OrdersController.LimitedOrderMode.LOMODE_SELLANDBUY);
    }//GEN-LAST:event_comboBoxLimitedModeActionPerformed

    private void checkboxAutoOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxAutoOrderActionPerformed
        coinRatingController.setAutoOrder(checkboxAutoOrder.isSelected());
    }//GEN-LAST:event_checkboxAutoOrderActionPerformed

    private void buttonRatingStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRatingStartActionPerformed
        initAPI();
        coinRatingController.getCoinCycleController().setRestrictCoins(textFieldRestrictedCoins.getText());
        coinRatingController.getCoinCycleController().setRequiredCoins(textFieldRequiredCoins.getText());
        coinRatingController.getCoinCycleController().setMainCoins(textFieldCycleMainCoins.getText());
        coinRatingController.getCoinCycleController().setDelayTime(((Number) spinnerCycleDelay.getValue()).longValue());
        coinRatingController.getCoinCycleController().setStopLimitTimeout(((Number) spinnerCycleAbortTime.getValue()).intValue());
        coinRatingController.getCoinCycleController().setStopFirstLimitTimeout(((Number) spinnerCycleFirstAbortTime.getValue()).intValue());
        coinRatingController.getCoinCycleController().setSwitchLimitTimeout(((Number) spinnerCycleSwitchTime.getValue()).intValue());
        coinRatingController.getCoinCycleController().setMaxSwitchesCount(((Number) spinnerCycleMaxSwitches.getValue()).intValue());
        coinRatingController.getCoinCycleController().setTradingBalancePercent(BigDecimal.valueOf(((Number) spinnerCyclePercent.getValue()).doubleValue()));
        coinRatingController.getCoinCycleController().setMinProfitPercent(((Number) spinnerCycleMinProfitPercent.getValue()).doubleValue());
        coinRatingController.getCoinCycleController().setMaxGlobalCoinRank(((Number) spinnerCycleMaxCoinRank.getValue()).doubleValue());
        coinRatingController.getCoinCycleController().setMinPairRating(((Number) spinnerCycleMinPairRating.getValue()).doubleValue());
        coinRatingController.getCoinCycleController().setMinPairBaseHourVolume(((Number) spinnerCycleMinBaseVolume.getValue()).doubleValue());
        coinRatingController.getCoinCycleController().setMaxActiveCyclesCount(((Number) spinnerCycleMaxEnterCount.getValue()).intValue());
        coinRatingController.getCoinCycleController().setUseDepsetUpdates(checkBoxCycleDepthCheck.isSelected());
        coinRatingController.setUpdateTrendTime(((Number) spinnerScanTrendUpdateTime.getValue()).longValue());
        coinRatingController.setUpdateRanksTime(((Number) spinnerScanRanksUpdateTime.getValue()).longValue());
        coinRatingController.setLowHold(checkBoxLowHold.isSelected());
        coinRatingController.setAutoOrder(checkboxAutoOrder.isSelected());
        coinRatingController.setAutoFastOrder(checkboxAutoFastorder.isSelected());
        coinRatingController.setNoAutoBuysOnDowntrend(checkBoxDowntrendNoAuto.isSelected());
        coinRatingController.setUseSignals(checkBoxUseSignals.isSelected());
        coinRatingController.setUseCycles(checkBoxUseCycles.isSelected());
        coinRatingController.getSignalOrderController().setAutoSignalOrder(checkboxAutoSignalOrder.isSelected());
        coinRatingController.getSignalOrderController().setAutoSignalFastOrder(checkboxAutoSignalFastorder.isSelected());
        coinRatingController.setAnalyzer(checkBoxAutoAnalyzer.isSelected());
        coinRatingController.setDelayTime(((Number) spinnerScanRatingDelayTime.getValue()).longValue());
        coinRatingController.setUpdateTime(((Number) spinnerScanRatingUpdateTime.getValue()).longValue());
        coinRatingController.getAutoOrders().setMaxEnter(((Number) spinnerRatingMaxOrders.getValue()).intValue());
        coinRatingController.getAutoOrders().setSecondsOrderEnterWait(((Number) spinnerRatingMaxOrderWait.getValue()).intValue());
        coinRatingController.setMinRatingForOrder(((Number) spinnerRatingMinForOrder.getValue()).floatValue());
        coinRatingController.getSignalOrderController().setMinSignalRatingForOrder(((Number) spinnerSignalRatingMinForOrder.getValue()).floatValue());
        coinRatingController.getSignalOrderController().getSignalController().setPreloadCount(((Number) spinnerSignalPreloadCount.getValue()).intValue());
        coinRatingController.setProgressBar(progressBarRatingAnalPercent);
        comboBoxRatingSortActionPerformed(null);
        comboBoxRatingSortbyActionPerformed(null);
        coinRatingController.getSignalOrderController().getSignalController().setApiId(textFieldAPIID.getText());
        coinRatingController.getSignalOrderController().getSignalController().setApiHash(textFieldAPIHash.getText());
        coinRatingController.getSignalOrderController().getSignalController().setApiPhone(textFieldPhone.getText());
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
        if (buttonRun.isEnabled()) CoinInfoAggregator.getInstance().doStop();
    }//GEN-LAST:event_buttonRatingStopActionPerformed

    private void comboBoxRatingSortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxRatingSortActionPerformed
        coinRatingController.setSortAsc(comboBoxRatingSort.getSelectedIndex() == 0);
    }//GEN-LAST:event_comboBoxRatingSortActionPerformed

    private void comboBoxRatingSortbyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxRatingSortbyActionPerformed
        int sindex = comboBoxRatingSortby.getSelectedIndex();
        if (sindex < 0) sindex = 0;
        else if (sindex > CoinRatingSort.values().length-1) sindex = CoinRatingSort.values().length-1;
        coinRatingController.setSortby(CoinRatingSort.values()[sindex]);
    }//GEN-LAST:event_comboBoxRatingSortbyActionPerformed

    private void spinnerScanRatingDelayTimeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScanRatingDelayTimeStateChanged
        coinRatingController.setDelayTime(((Number) spinnerScanRatingDelayTime.getValue()).longValue());
    }//GEN-LAST:event_spinnerScanRatingDelayTimeStateChanged

    private void buttonNNBTrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNNBTrainActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "NNBTRAIN");
    }//GEN-LAST:event_buttonNNBTrainActionPerformed

    private void buttonNNCTrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNNCTrainActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "NNCTRAIN");
    }//GEN-LAST:event_buttonNNCTrainActionPerformed

    private void buttonNNBAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNNBAddActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "NNBADD");
    }//GEN-LAST:event_buttonNNBAddActionPerformed

    private void buttonNNCAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNNCAddActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "NNCADD");
    }//GEN-LAST:event_buttonNNCAddActionPerformed

    private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveActionPerformed
        pairProcessController.pairAction(listCurrencies.getSelectedIndex(), "REMOVE");
    }//GEN-LAST:event_buttonRemoveActionPerformed

    private void checkboxAutoFastorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxAutoFastorderActionPerformed
        coinRatingController.setAutoFastOrder(checkboxAutoFastorder.isSelected());
    }//GEN-LAST:event_checkboxAutoFastorderActionPerformed

    private void buttonTlgConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTlgConnectActionPerformed
        SignalController ccc = new SignalController();
        ccc.setApiId(textFieldAPIID.getText());
        ccc.setApiHash(textFieldAPIHash.getText());
        ccc.setApiPhone(textFieldPhone.getText());
        ccc.startSignalsProcess(true);
    }//GEN-LAST:event_buttonTlgConnectActionPerformed

    private void checkboxAutoSignalOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxAutoSignalOrderActionPerformed
        coinRatingController.getSignalOrderController().setAutoSignalOrder(checkboxAutoSignalOrder.isSelected());
    }//GEN-LAST:event_checkboxAutoSignalOrderActionPerformed

    private void checkboxAutoSignalFastorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxAutoSignalFastorderActionPerformed
        coinRatingController.getSignalOrderController().setAutoSignalFastOrder(checkboxAutoSignalFastorder.isSelected());
    }//GEN-LAST:event_checkboxAutoSignalFastorderActionPerformed

    private void checkBoxWalkForwardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxWalkForwardActionPerformed
        OrdersController.getInstance().setAutoWalkForward(checkBoxWalkForward.isSelected());
    }//GEN-LAST:event_checkBoxWalkForwardActionPerformed

    private void checkBoxAutoStrategyParamsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAutoStrategyParamsActionPerformed
        OrdersController.getInstance().setAutoPickStrategyParams(checkBoxAutoStrategyParams.isSelected());
    }//GEN-LAST:event_checkBoxAutoStrategyParamsActionPerformed

    private void checkBoxDowntrendNoAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxDowntrendNoAutoActionPerformed
        coinRatingController.setNoAutoBuysOnDowntrend(checkBoxDowntrendNoAuto.isSelected());
    }//GEN-LAST:event_checkBoxDowntrendNoAutoActionPerformed

    private void checkBoxUseSignalsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUseSignalsActionPerformed
        coinRatingController.setUseSignals(checkBoxUseSignals.isSelected());
    }//GEN-LAST:event_checkBoxUseSignalsActionPerformed

    private void checkBoxUseCyclesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUseCyclesActionPerformed
        coinRatingController.setUseCycles(checkBoxUseCycles.isSelected());
    }//GEN-LAST:event_checkBoxUseCyclesActionPerformed

    private void ButtonShowLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonShowLogActionPerformed
        logsForm.setVisible(true);
    }//GEN-LAST:event_ButtonShowLogActionPerformed

    private void checkboxTestModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxTestModeActionPerformed
        checkBoxTestAdd.setEnabled(checkboxTestMode.isSelected());
        spinnerTestAdd.setEnabled(checkboxTestMode.isSelected());
        textFieldTestCoinAdd.setEnabled(checkboxTestMode.isSelected());
    }//GEN-LAST:event_checkboxTestModeActionPerformed

    private void checkBoxPyramidingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPyramidingActionPerformed
        spinnerPyramidingMaxC.setEnabled(checkBoxPyramiding.isSelected());
        labelAfterPyramidingMax.setEnabled(checkBoxPyramiding.isSelected());
    }//GEN-LAST:event_checkBoxPyramidingActionPerformed

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
    private javax.swing.JButton ButtonShowLog;
    private javax.swing.JButton ButtonStatistics;
    private javax.swing.JComboBox<String> ComboBoxMainStrategy;
    private javax.swing.JButton buttonBuy;
    private javax.swing.JButton buttonCancelLimit;
    private javax.swing.JButton buttonNNBAdd;
    private javax.swing.JButton buttonNNBTrain;
    private javax.swing.JButton buttonNNCAdd;
    private javax.swing.JButton buttonNNCTrain;
    private javax.swing.JButton buttonPause;
    private javax.swing.JButton buttonRatingCheck;
    private javax.swing.JButton buttonRatingStart;
    private javax.swing.JButton buttonRatingStop;
    private javax.swing.JButton buttonRemove;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonSell;
    private javax.swing.JButton buttonSetPairs;
    private javax.swing.JButton buttonShowPlot;
    private javax.swing.JButton buttonStop;
    private javax.swing.JButton buttonTlgConnect;
    private javax.swing.JButton buttonUpdate;
    private javax.swing.JButton buttonWebBrowserOpen;
    private javax.swing.JCheckBox checkBoxAutoAnalyzer;
    private javax.swing.JCheckBox checkBoxAutoStrategyParams;
    private javax.swing.JCheckBox checkBoxBuyStopLimited;
    private javax.swing.JCheckBox checkBoxCheckOtherStrategies;
    private javax.swing.JCheckBox checkBoxCycleDepthCheck;
    private javax.swing.JCheckBox checkBoxDowntrendNoAuto;
    private javax.swing.JCheckBox checkBoxLimitedOrders;
    private javax.swing.JCheckBox checkBoxLowHold;
    private javax.swing.JCheckBox checkBoxPyramiding;
    private javax.swing.JCheckBox checkBoxSellStopLimited;
    private javax.swing.JCheckBox checkBoxStopGain;
    private javax.swing.JCheckBox checkBoxStopLoss;
    private javax.swing.JCheckBox checkBoxTestAdd;
    private javax.swing.JCheckBox checkBoxUseCycles;
    private javax.swing.JCheckBox checkBoxUseSignals;
    private javax.swing.JCheckBox checkBoxWalkForward;
    private javax.swing.JCheckBox checkBoxWavesUse;
    private javax.swing.JCheckBox checkboxAutoFastorder;
    private javax.swing.JCheckBox checkboxAutoOrder;
    private javax.swing.JCheckBox checkboxAutoSignalFastorder;
    private javax.swing.JCheckBox checkboxAutoSignalOrder;
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
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JLabel labelAccountCost;
    private javax.swing.JLabel labelAfterPyramidingMax;
    private javax.swing.JLabel labelDownTrend;
    private javax.swing.JLabel labelMCBTCDominance;
    private javax.swing.JLabel labelMCCap;
    private javax.swing.JLabel labelMCVolume;
    private javax.swing.JLabel labelUpTrend;
    private javax.swing.JList<String> listBoxAutoStrategies;
    private javax.swing.JList<String> listCurrencies;
    private javax.swing.JList<String> listProfit;
    private javax.swing.JList<String> listRating;
    private javax.swing.JProgressBar progressBarRatingAnalPercent;
    private javax.swing.JSpinner spinnerBuyMainValue;
    private javax.swing.JSpinner spinnerBuyPercent;
    private javax.swing.JSpinner spinnerBuyStopLimited;
    private javax.swing.JSpinner spinnerBuyWavesFirstMainValue;
    private javax.swing.JSpinner spinnerBuyWavesFirstPercent;
    private javax.swing.JSpinner spinnerBuyWavesIncKoef;
    private javax.swing.JSpinner spinnerBuyWavesSecondPercent;
    private javax.swing.JSpinner spinnerCycleAbortTime;
    private javax.swing.JSpinner spinnerCycleDelay;
    private javax.swing.JSpinner spinnerCycleFirstAbortTime;
    private javax.swing.JSpinner spinnerCycleMaxCoinRank;
    private javax.swing.JSpinner spinnerCycleMaxEnterCount;
    private javax.swing.JSpinner spinnerCycleMaxSwitches;
    private javax.swing.JSpinner spinnerCycleMinBaseVolume;
    private javax.swing.JSpinner spinnerCycleMinPairRating;
    private javax.swing.JSpinner spinnerCycleMinProfitPercent;
    private javax.swing.JSpinner spinnerCyclePercent;
    private javax.swing.JSpinner spinnerCycleSwitchTime;
    private javax.swing.JSpinner spinnerPricesUpdateDelay;
    private javax.swing.JSpinner spinnerPyramidingMaxC;
    private javax.swing.JSpinner spinnerRatingMaxOrderWait;
    private javax.swing.JSpinner spinnerRatingMaxOrders;
    private javax.swing.JSpinner spinnerRatingMinBaseVolume;
    private javax.swing.JSpinner spinnerRatingMinForOrder;
    private javax.swing.JSpinner spinnerScanRanksUpdateTime;
    private javax.swing.JSpinner spinnerScanRatingDelayTime;
    private javax.swing.JSpinner spinnerScanRatingUpdateTime;
    private javax.swing.JSpinner spinnerScanTrendUpdateTime;
    private javax.swing.JSpinner spinnerSellStopLimited;
    private javax.swing.JSpinner spinnerSignalPreloadCount;
    private javax.swing.JSpinner spinnerSignalRatingMinForOrder;
    private javax.swing.JSpinner spinnerStopGain;
    private javax.swing.JSpinner spinnerStopLoss;
    private javax.swing.JSpinner spinnerTestAdd;
    private javax.swing.JSpinner spinnerUpdateDelay;
    private javax.swing.JSpinner spinnerWavesHalfProfitOrdersCnt;
    private javax.swing.JSpinner spinnerWavesInitialProfitPercent;
    private javax.swing.JSpinner spinnerWavesMinProfitPercent;
    private javax.swing.JTextField textFieldAPIHash;
    private javax.swing.JTextField textFieldAPIID;
    private javax.swing.JTextField textFieldApiKey;
    private javax.swing.JTextField textFieldApiSecret;
    private javax.swing.JTextField textFieldBaseCoin;
    private javax.swing.JTextField textFieldCycleMainCoins;
    private javax.swing.JTextField textFieldPhone;
    private javax.swing.JTextField textFieldRequiredCoins;
    private javax.swing.JTextField textFieldRestrictedCoins;
    private javax.swing.JTextField textFieldTestCoinAdd;
    private javax.swing.JTextField textFieldTradePairs;
    // End of variables declaration//GEN-END:variables

}
