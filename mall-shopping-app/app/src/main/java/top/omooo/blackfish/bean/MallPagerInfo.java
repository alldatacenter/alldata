package top.omooo.blackfish.bean;

import java.util.List;

/**
 * Created by SSC on 2018/4/7.
 */

public class MallPagerInfo {
    private List<BannerInfo> mBannerInfos;
    private List<GridInfo> mClassifyInfos;
    private String singleImageUrl;
    private List<BannerInfo> mGridGoodsInfos;
    private List<MallGoodsInfo> mMallGoodsInfos;
    private List<RecommendGoodsInfo> mRecommendGoodsInfos;

    public MallPagerInfo(List<BannerInfo> bannerInfos, List<GridInfo> classifyInfos, String singleImageUrl, List<BannerInfo> gridGoodsInfos, List<MallGoodsInfo> mallGoodsInfos, List<RecommendGoodsInfo> recommendGoodsInfos) {
        mBannerInfos = bannerInfos;
        mClassifyInfos = classifyInfos;
        this.singleImageUrl = singleImageUrl;
        mGridGoodsInfos = gridGoodsInfos;
        mMallGoodsInfos = mallGoodsInfos;
        mRecommendGoodsInfos = recommendGoodsInfos;
    }

    public List<BannerInfo> getBannerInfos() {
        return mBannerInfos;
    }

    public void setBannerInfos(List<BannerInfo> bannerInfos) {
        mBannerInfos = bannerInfos;
    }

    public List<GridInfo> getClassifyInfos() {
        return mClassifyInfos;
    }

    public void setClassifyInfos(List<GridInfo> classifyInfos) {
        mClassifyInfos = classifyInfos;
    }

    public String getSingleImageUrl() {
        return singleImageUrl;
    }

    public void setSingleImageUrl(String singleImageUrl) {
        this.singleImageUrl = singleImageUrl;
    }

    public List<BannerInfo> getGridGoodsInfos() {
        return mGridGoodsInfos;
    }

    public void setGridGoodsInfos(List<BannerInfo> gridGoodsInfos) {
        mGridGoodsInfos = gridGoodsInfos;
    }

    public List<MallGoodsInfo> getMallGoodsInfos() {
        return mMallGoodsInfos;
    }

    public void setMallGoodsInfos(List<MallGoodsInfo> mallGoodsInfos) {
        mMallGoodsInfos = mallGoodsInfos;
    }

    public List<RecommendGoodsInfo> getRecommendGoodsInfos() {
        return mRecommendGoodsInfos;
    }

    public void setRecommendGoodsInfos(List<RecommendGoodsInfo> recommendGoodsInfos) {
        mRecommendGoodsInfos = recommendGoodsInfos;
    }
}
