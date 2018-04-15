/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evgcompany.binntrdbot.strategies;

import com.evgcompany.binntrdbot.strategies.core.*;
import com.evgcompany.binntrdbot.analysis.*;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Decimal;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;
import org.ta4j.core.trading.rules.NotRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

/**
 *
 * @author EVG_Adminer
 */
public class StrategySignal extends StrategyItem {

    public StrategySignal(StrategiesController controller) {
        super(controller);
        StrategyName = "Signal";
        config.Add("Price1", new StrategyConfigItem("0", "100000000", "0.000000001", "0")).setActive(false);
        config.Add("Price2", new StrategyConfigItem("0", "100000000", "0.000000001", "0")).setActive(false);
        config.Add("PriceTarget", new StrategyConfigItem("0", "100000000", "0.000000001", "0")).setActive(false);
        config.Add("PriceStop", new StrategyConfigItem("0", "100000000", "0.000000001", "0")).setActive(false);
    }

    private Decimal getLossPercent(Decimal priceC, Decimal priceS) {
        Decimal loss_percent = priceC.minus(priceS).dividedBy(priceC).multipliedBy(Decimal.HUNDRED);
        if (loss_percent.compareTo(Decimal.valueOf(10)) > 0) {
            loss_percent = Decimal.valueOf(10);
        } else if (loss_percent.compareTo(Decimal.valueOf(2)) < 0) {
            loss_percent = Decimal.valueOf(2);
        }
        return loss_percent;
    }

    @Override
    public Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        
        Decimal price1 = config.GetNumValue("Price1");
        Decimal price2 = config.GetNumValue("Price2");
        Decimal priceT = config.GetNumValue("PriceTarget");
        Decimal priceS = config.GetNumValue("PriceStop");
        Decimal price2u = price2.multipliedBy(90).plus(priceT.multipliedBy(10)).dividedBy(100);
        Decimal loss_percent = getLossPercent(price1, priceS);
        
        initializer = (tseries, trecord, dataset) -> {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(tseries);
            TrailingStopLossIndicator stoploss = new TrailingStopLossIndicator(closePrice, loss_percent, priceS, trecord);
            ConstantIndicator p1 = new ConstantIndicator(price1);
            ConstantIndicator p2 = new ConstantIndicator(price2);
            ConstantIndicator pT = new ConstantIndicator(priceT);
            ConstantIndicator pS = new ConstantIndicator(priceS);
            dataset.addSeries(buildChartTimeSeries(tseries, p1, "Price1"));
            dataset.addSeries(buildChartTimeSeries(tseries, p2, "Price2"));
            dataset.addSeries(buildChartTimeSeries(tseries, pT, "Price target"));
            dataset.addSeries(buildChartTimeSeries(tseries, pS, "Price stoploss"));
            dataset.addSeries(buildChartTimeSeries(tseries, stoploss, "Trailing stoploss"));
        };
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        Rule entryRule = new UnderIndicatorRule(closePrice, price2u)
                .and(new OverIndicatorRule(closePrice, priceS))
                .and(new IsRisingRule(closePrice, 1))
                .and(new NotRule(new HasOpenedOrderRule()));
        
        Rule exitRule = new OverIndicatorRule(closePrice, priceT)
                .and(new IsFallingRule(closePrice, 2))
                //.or(new TrailingStopLossRule(closePrice, loss_percent, priceS))
                .or(new StopLossRule(closePrice, priceS));
        
        return new BaseStrategy(entryRule, exitRule);
    }
}