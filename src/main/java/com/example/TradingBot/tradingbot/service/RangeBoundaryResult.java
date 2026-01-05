package com.example.TradingBot.tradingbot.service;

public class RangeBoundaryResult {
    private  double upperBound;
    private  double lowerBound;

    public RangeBoundaryResult() {
    }

    public RangeBoundaryResult(double upperBound, double lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }
}
