package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/4/8.
 */

public class MallHotClassifyGridInfo {
    private String headerImageUrl;
    private String goodsDesc;
    private String goodsPeriods;
    private String goodsPrice;

    public MallHotClassifyGridInfo(String headerImageUrl, String goodsDesc, String goodsPeriods, String goodsPrice) {
        this.headerImageUrl = headerImageUrl;
        this.goodsDesc = goodsDesc;
        this.goodsPeriods = goodsPeriods;
        this.goodsPrice = goodsPrice;
    }

    public String getHeaderImageUrl() {
        return headerImageUrl;
    }

    public void setHeaderImageUrl(String headerImageUrl) {
        this.headerImageUrl = headerImageUrl;
    }

    public String getGoodsDesc() {
        return goodsDesc;
    }

    public void setGoodsDesc(String goodsDesc) {
        this.goodsDesc = goodsDesc;
    }

    public String getGoodsPeriods() {
        return goodsPeriods;
    }

    public void setGoodsPeriods(String goodsPeriods) {
        this.goodsPeriods = goodsPeriods;
    }

    public String getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(String goodsPrice) {
        this.goodsPrice = goodsPrice;
    }
}
