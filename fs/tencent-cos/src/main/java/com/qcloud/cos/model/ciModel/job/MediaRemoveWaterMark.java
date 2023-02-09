package com.qcloud.cos.model.ciModel.job;


/**
 * 去除水印实体类
 */
public class MediaRemoveWaterMark {
    /**
     * 距离左上角原点x偏移, [1, 4096]
     */
    private String dx;
    /**
     * 距离左上角原点y偏移, [1, 4096]
     */
    private String dy;
    /**
     * 宽, [1, 4096]
     */
    private String width;
    /**
     * 高, [1, 4096]
     */
    private String height;

    /**
     * 开关
     */
    private String _switch;

    public String getDx() {
        return dx;
    }

    public void setDx(String dx) {
        this.dx = dx;
    }

    public String getDy() {
        return dy;
    }

    public void setDy(String dy) {
        this.dy = dy;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }


    public String get_switch() {
        return _switch;
    }

    public void set_switch(String _switch) {
        this._switch = _switch;
    }

    @Override
    public String toString() {
        return "MediaRemoveWaterMark{" +
                "dx='" + dx + '\'' +
                ", dy='" + dy + '\'' +
                ", width='" + width + '\'' +
                ", height='" + height + '\'' +
                '}';
    }
}
