package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/3/3.
 */

import java.io.Serializable;
import java.util.List;

/**
 * 首页下拉三个商品分类信息
 */
public class HomeSortInfo  implements Serializable{
    private String title;   //标题
    private String sortImageUrl;    //分类图片Url
    private List<HomeSortItemInfo> mItemInfos;  //分类商品信息

    public HomeSortInfo(String title, String sortImageUrl, List<HomeSortItemInfo> itemInfos) {
        this.title = title;
        this.sortImageUrl = sortImageUrl;
        mItemInfos = itemInfos;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSortImageUrl() {
        return sortImageUrl;
    }

    public void setSortImageUrl(String sortImageUrl) {
        this.sortImageUrl = sortImageUrl;
    }

    public List<HomeSortItemInfo> getItemInfos() {
        return mItemInfos;
    }

    public void setItemInfos(List<HomeSortItemInfo> itemInfos) {
        mItemInfos = itemInfos;
    }
}
