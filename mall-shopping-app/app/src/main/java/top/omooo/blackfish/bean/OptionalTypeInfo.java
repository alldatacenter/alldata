package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/4/13.
 */

public class OptionalTypeInfo {
    private String type;
    private double totalPrice;
    private double singlePrice;

    public OptionalTypeInfo(String type, double totalPrice, double singlePrice) {
        this.type = type;
        this.totalPrice = totalPrice;
        this.singlePrice = singlePrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getSinglePrice() {
        return singlePrice;
    }

    public void setSinglePrice(double singlePrice) {
        this.singlePrice = singlePrice;
    }
}
