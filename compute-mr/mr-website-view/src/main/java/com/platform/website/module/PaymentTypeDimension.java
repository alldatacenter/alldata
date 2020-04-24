package com.platform.website.module;

/**
 * 支付方式dimension类
 * 
 * @author wulinhao
 *
 */
public class PaymentTypeDimension {
    private int id;
    private String paymentType;

    public PaymentTypeDimension() {
        super();
    }

    public PaymentTypeDimension(int id) {
        super();
        this.id = id;
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
}
