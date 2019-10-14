package top.omooo.blackfish.bean;

import java.util.List;

/**
 * Created by SSC on 2018/4/7.
 */

public class MallGoodsInfo {
    private String headerImageUrl;
    private List<MallGoodsItemInfo> mMallGoodsItemInfos;

    public MallGoodsInfo(String headerImageUrl, List<MallGoodsItemInfo> mallGoodsItemInfos) {
        this.headerImageUrl = headerImageUrl;
        mMallGoodsItemInfos = mallGoodsItemInfos;
    }

    public String getHeaderImageUrl() {
        return headerImageUrl;
    }

    public void setHeaderImageUrl(String headerImageUrl) {
        this.headerImageUrl = headerImageUrl;
    }

    public List<MallGoodsItemInfo> getMallGoodsItemInfos() {
        return mMallGoodsItemInfos;
    }

    public void setMallGoodsItemInfos(List<MallGoodsItemInfo> mallGoodsItemInfos) {
        mMallGoodsItemInfos = mallGoodsItemInfos;
    }
}
