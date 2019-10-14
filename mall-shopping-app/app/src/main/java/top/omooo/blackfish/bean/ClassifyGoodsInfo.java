package top.omooo.blackfish.bean;

import java.util.List;

/**
 * Created by SSC on 2018/4/5.
 */

/**
 *  ClassifyGoodsActivity 的数据类
 */
public class ClassifyGoodsInfo {
    private String title;   //标题
    private String headerImageUrl;  //头部图片
    private String subtitle1;   //一般值为常用分类
    private String subtitle2;   //一般值为热门分类
    private List<ClassifyGridInfo> gridImageUrls1;  //常用分类GridView数据
    private List<ClassifyGridInfo> gridImageUrls2;     //热门分类GridView数据


    public ClassifyGoodsInfo(String title, String headerImageUrl, String subtitle1, String subtitle2, List<ClassifyGridInfo> gridImageUrls1, List<ClassifyGridInfo> gridImageUrls2) {
        this.title = title;
        this.headerImageUrl = headerImageUrl;
        this.subtitle1 = subtitle1;
        this.subtitle2 = subtitle2;
        this.gridImageUrls1 = gridImageUrls1;
        this.gridImageUrls2 = gridImageUrls2;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHeaderImageUrl() {
        return headerImageUrl;
    }

    public void setHeaderImageUrl(String headerImageUrl) {
        this.headerImageUrl = headerImageUrl;
    }

    public String getSubtitle1() {
        return subtitle1;
    }

    public void setSubtitle1(String subtitle1) {
        this.subtitle1 = subtitle1;
    }

    public String getSubtitle2() {
        return subtitle2;
    }

    public void setSubtitle2(String subtitle2) {
        this.subtitle2 = subtitle2;
    }

    public List<ClassifyGridInfo> getGridImageUrls1() {
        return gridImageUrls1;
    }

    public void setGridImageUrls1(List<ClassifyGridInfo> gridImageUrls1) {
        this.gridImageUrls1 = gridImageUrls1;
    }

    public List<ClassifyGridInfo> getGridImageUrls2() {
        return gridImageUrls2;
    }

    public void setGridImageUrls2(List<ClassifyGridInfo> gridImageUrls2) {
        this.gridImageUrls2 = gridImageUrls2;
    }
}
