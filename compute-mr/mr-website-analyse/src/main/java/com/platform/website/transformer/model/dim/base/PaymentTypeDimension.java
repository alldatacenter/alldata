package com.platform.website.transformer.model.dim.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 支付方式dimension类
 * 
 * @author wulinhao
 *
 */
public class PaymentTypeDimension extends BaseDimension {
    private int id;
    private String paymentType;

    public PaymentTypeDimension() {
        super();
    }

    

    public PaymentTypeDimension(String paymentType) {
        super();
        this.paymentType = paymentType;
    }



    public PaymentTypeDimension(int id, String paymentType) {
        super();
        this.id = id;
        this.paymentType = paymentType;
    }

    public int getId() {
        return id;
    }



    public void setId(int id) {
        this.id = id;
    }



    public String getPaymentType() {
        return paymentType;
    }



    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.paymentType);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.paymentType = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        PaymentTypeDimension other = (PaymentTypeDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }

        tmp = this.paymentType.compareTo(other.paymentType);
        return tmp;
    }

}
