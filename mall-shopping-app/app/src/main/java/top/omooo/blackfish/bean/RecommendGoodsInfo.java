package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/4/11.
 */

public class RecommendGoodsInfo {
    private String imageUrl;
    private String desc;
    private double singlePrice;
    private int periods;
    private double totalPrice;
    private String evaluation;

    public RecommendGoodsInfo(String imageUrl, String desc, double singlePrice, int periods, double totalPrice, String evaluation) {
        this.imageUrl = imageUrl;
        this.desc = desc;
        this.singlePrice = singlePrice;
        this.periods = periods;
        this.totalPrice = totalPrice;
        this.evaluation = evaluation;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getSinglePrice() {
        return singlePrice;
    }

    public void setSinglePrice(double singlePrice) {
        this.singlePrice = singlePrice;
    }

    public int getPeriods() {
        return periods;
    }

    public void setPeriods(int periods) {
        this.periods = periods;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}
