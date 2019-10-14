package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/4/13.
 */

public class SimilarRecoInfo {
    private String imageUrl;
    private String desc;
    private double totalPrice;
    private double singlePrice;
    private int periods;

    public SimilarRecoInfo(String imageUrl, String desc, double totalPrice, double singlePrice, int periods) {
        this.imageUrl = imageUrl;
        this.desc = desc;
        this.totalPrice = totalPrice;
        this.singlePrice = singlePrice;
        this.periods = periods;
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

    public int getPeriods() {
        return periods;
    }

    public void setPeriods(int periods) {
        this.periods = periods;
    }
}
