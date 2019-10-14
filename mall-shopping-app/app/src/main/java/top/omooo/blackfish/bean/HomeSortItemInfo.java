package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/3/3.
 */

/**
 * 首页分类商品信息
 */
public class HomeSortItemInfo {
    private String id;
    private String goodsImageUrl;

    public HomeSortItemInfo(String id, String goodsImageUrl) {
        this.id = id;
        this.goodsImageUrl = goodsImageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoodsImageUrl() {
        return goodsImageUrl;
    }

    public void setGoodsImageUrl(String goodsImageUrl) {
        this.goodsImageUrl = goodsImageUrl;
    }
}
