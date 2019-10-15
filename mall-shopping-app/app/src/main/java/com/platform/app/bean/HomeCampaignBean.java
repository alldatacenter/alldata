package com.platform.app.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;

/**
 * Created by wulinhao
 * Time  2019/8/7
 * Describe: 首页 商品数据
 */

public class HomeCampaignBean implements MultiItemEntity {

    /**
     * item为大图在左边的
     */
    public static final int ITEM_TYPE_LEFT  = 0;

    /**
     * item为大图在右边的
     */
    public static final int ITEM_TYPE_RIGHT = 1;

    private Campaign cpOne;
    private Campaign cpTwo;
    private Campaign cpThree;
    private int      id;
    private String   title;
    private int      itemType;

    public HomeCampaignBean(int itemType) {
        this.itemType = itemType;
    }

    public Campaign getCpOne() {
        return cpOne;
    }

    public void setCpOne(Campaign cpOne) {
        this.cpOne = cpOne;
    }

    public Campaign getCpTwo() {
        return cpTwo;
    }

    public void setCpTwo(Campaign cpTwo) {
        this.cpTwo = cpTwo;
    }

    public Campaign getCpThree() {
        return cpThree;
    }

    public void setCpThree(Campaign cpThree) {
        this.cpThree = cpThree;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }
}
