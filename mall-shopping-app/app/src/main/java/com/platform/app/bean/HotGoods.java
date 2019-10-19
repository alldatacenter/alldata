package com.platform.app.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by wulinhao
 * Time  2019/9/8
 * Describe:  热卖商品.也是 一级菜单  二级菜单的数据模型
 *            同时,也是首页跳转到商品列表 的数据模型
 */

public class HotGoods {

    private String           copyright;
    private int              totalCount;
    private int              currentPage;
    private int              totalPage;
    private int              pageSize;
    private List<OrdersBean> orders;
    private List<ListBean>   list;

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<OrdersBean> getOrders() {
        return orders;
    }

    public void setOrders(List<OrdersBean> orders) {
        this.orders = orders;
    }

    public List<ListBean> getList() {
        return list;
    }

    public void setList(List<ListBean> list) {
        this.list = list;
    }


    public static class OrdersBean {
        /**
         * orderType : DESC
         * field : id
         */

        private String orderType;
        private String field;

        public String getOrderType() {
            return orderType;
        }

        public void setOrderType(String orderType) {
            this.orderType = orderType;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    public static class ListBean implements Serializable {


        /**
         * id : 1
         * categoryId : 5
         * campaignId : 1
         * name : 联想（Lenovo）拯救者14.0英寸游戏本（i7-4720HQ 4G 1T硬盘 GTX960M 2G独显 FHD IPS屏 背光键盘）黑
         * imgUrl : http://7mno4h.com2.z0.glb.qiniucdn.com/s_recommend_55c1e8f7N4b99de71.jpg
         * price : 5979
         * sale : 8654
         */

        private int    id;
        private int    categoryId;
        private int    campaignId;
        private String name;
        private String imgUrl;
        private double price;
        private double sale;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(int categoryId) {
            this.categoryId = categoryId;
        }

        public int getCampaignId() {
            return campaignId;
        }

        public void setCampaignId(int campaignId) {
            this.campaignId = campaignId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public double getSale() {
            return sale;
        }

        public void setSale(double sale) {
            this.sale = sale;
        }
    }
}
