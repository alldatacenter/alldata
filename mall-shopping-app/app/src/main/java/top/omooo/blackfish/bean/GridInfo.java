package top.omooo.blackfish.bean;

/**
 * Created by SSC on 2018/4/7.
 */

public class GridInfo {
    private String title;
    private String imageUrl;

    public GridInfo(String title, String imageUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
