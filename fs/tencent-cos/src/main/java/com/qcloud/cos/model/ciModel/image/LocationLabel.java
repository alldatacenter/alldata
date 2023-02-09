package com.qcloud.cos.model.ciModel.image;

/**
 * 标签信息V2
 */
public class LocationLabel extends Lobel {

    /**
     * 三级商品分类对应的一级分类和二级分类，两级之间用“_”（下划线）隔开，例如商品名称是“计算机键盘”，那么Parents输出为“物品_数码产品”
     */
    private String parents;

    /**
     * 坐标X轴的最大值
     */
    private String xMax;
    /**
     * 坐标X轴的最小值
     */
    private String xMin;
    /**
     * 坐标y轴的最大值
     */
    private String yMax;
    /**
     * 坐标y轴的最小值
     */
    private String yMin;

    public String getParents() {
        return parents;
    }

    public void setParents(String parents) {
        this.parents = parents;
    }

    public String getxMax() {
        return xMax;
    }

    public void setxMax(String xMax) {
        this.xMax = xMax;
    }

    public String getxMin() {
        return xMin;
    }

    public void setxMin(String xMin) {
        this.xMin = xMin;
    }

    public String getyMax() {
        return yMax;
    }

    public void setyMax(String yMax) {
        this.yMax = yMax;
    }

    public String getyMin() {
        return yMin;
    }

    public void setyMin(String yMin) {
        this.yMin = yMin;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LocationLabel{");
        sb.append("parents='").append(parents).append('\'');
        sb.append(", xMax='").append(xMax).append('\'');
        sb.append(", xMin='").append(xMin).append('\'');
        sb.append(", yMax='").append(yMax).append('\'');
        sb.append(", yMin='").append(yMin).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
