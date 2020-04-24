package com.platform.website.transformer.model.dim.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 货币类型dimension类
 * 
 * @author wulinhao
 *
 */
public class CurrencyTypeDimension extends BaseDimension {
    private int id;
    private String currencyName;

    public CurrencyTypeDimension() {
        super();
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

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.currencyName);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.currencyName = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        CurrencyTypeDimension other = (CurrencyTypeDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }

        tmp = this.currencyName.compareTo(other.currencyName);
        return tmp;
    }

}
