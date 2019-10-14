package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/3/2.
 */

/**
 * 首页轮播图(弃用)
 */
public class BannerItemInfo {
    private int imageBannerId;  //轮播图
    private int imagePointId;   //选中的点
    private int unImagePointId; //未选中的点

    public BannerItemInfo(int imageBannerId, int imagePointId,int unImagePointId) {
        this.imageBannerId = imageBannerId;
        this.imagePointId = imagePointId;
        this.unImagePointId = unImagePointId;
    }

    public int getImageBannerId() {
        return imageBannerId;
    }

    public void setImageBannerId(int imageBannerId) {
        this.imageBannerId = imageBannerId;
    }

    public int getImagePointId() {
        return imagePointId;
    }

    public void setImagePointId(int imagePointId) {
        this.imagePointId = imagePointId;
    }

    public int getUnImagePointId() {
        return unImagePointId;
    }

    public void setUnImagePointId(int unImagePointId) {
        this.unImagePointId = unImagePointId;
    }
}
