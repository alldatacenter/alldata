package com.platform.app.bean;


/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/06
 *     desc   :首页商品分类
 *     version: 1.0
 * </pre>
 */

public class HomeCategory extends Category {


    private int imgBig;
    private int imgSmallTop;
    private int imgSmallBottom;


    public HomeCategory(String name, int imgBig, int imgSmallTop, int imgSmallBottom) {
        super(name);
        this.imgBig = imgBig;
        this.imgSmallTop = imgSmallTop;
        this.imgSmallBottom = imgSmallBottom;
    }

    public int getImgBig() {
        return imgBig;
    }

    public void setImgBig(int imgBig) {
        this.imgBig = imgBig;
    }

    public int getImgSmallTop() {
        return imgSmallTop;
    }

    public void setImgSmallTop(int imgSmallTop) {
        this.imgSmallTop = imgSmallTop;
    }

    public int getImgSmallBottom() {
        return imgSmallBottom;
    }

    public void setImgSmallBottom(int imgSmallBottom) {
        this.imgSmallBottom = imgSmallBottom;
    }
}
