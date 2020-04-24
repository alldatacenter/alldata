package com.platform.website.module;

/**
 * 货币类型dimension类
 * 
 * @author wulinhao
 *
 */
public class CurrencyTypeDimension {
    private int id;
    private String currencyName;

    public CurrencyTypeDimension() {
        super();
    }

    public CurrencyTypeDimension(int id) {
        super();
        this.id = id;
    }

    public CurrencyTypeDimension(String currencyName) {
        super();
        this.currencyName = currencyName;
    }

    public CurrencyTypeDimension(int id, String currencyName) {
        super();
        this.id = id;
        this.currencyName = currencyName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
}
