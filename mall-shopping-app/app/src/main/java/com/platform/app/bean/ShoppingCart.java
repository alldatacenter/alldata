package com.platform.app.bean;

import java.io.Serializable;

/**
 * Created by wulinhao
 * Time  2019/8/9
 * Describe:  购物车商品信息.数据来源于商品数据.但多了数量、是否选中两个 属性
 */

public class ShoppingCart extends HotGoods.ListBean implements Serializable {

    private int count;
    private boolean isChecked = true;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

}
