package top.omooo.blackfish.bean;

import java.util.List;

/**
 * Created by SSC on 2018/4/13.
 */

public class GoodsDetailsInfo {
    private List<String> mBannerList;
    private double totalPrice;
    private double singlePrice;
    private int periods;
    private String desc;
    private String defaultType;
    private List<OptionalTypeInfo> mOptionalTypeInfos;
    private List<SimilarRecoInfo> mSimilarRecoInfos;

    public GoodsDetailsInfo(List<String> bannerList, double totalPrice, double singlePrice, int periods, String desc, String defaultType, List<OptionalTypeInfo> optionalTypeInfos, List<SimilarRecoInfo> similarRecoInfos) {
        mBannerList = bannerList;
        this.totalPrice = totalPrice;
        this.singlePrice = singlePrice;
        this.periods = periods;
        this.desc = desc;
        this.defaultType = defaultType;
        mOptionalTypeInfos = optionalTypeInfos;
        mSimilarRecoInfos = similarRecoInfos;
    }

    public List<String> getBannerList() {
        return mBannerList;
    }

    public void setBannerList(List<String> bannerList) {
        mBannerList = bannerList;
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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public List<OptionalTypeInfo> getOptionalTypeInfos() {
        return mOptionalTypeInfos;
    }

    public void setOptionalTypeInfos(List<OptionalTypeInfo> optionalTypeInfos) {
        mOptionalTypeInfos = optionalTypeInfos;
    }

    public List<SimilarRecoInfo> getSimilarRecoInfos() {
        return mSimilarRecoInfos;
    }

    public void setSimilarRecoInfos(List<SimilarRecoInfo> similarRecoInfos) {
        mSimilarRecoInfos = similarRecoInfos;
    }

}
