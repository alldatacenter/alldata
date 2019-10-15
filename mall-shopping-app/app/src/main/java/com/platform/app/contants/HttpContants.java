package com.platform.app.contants;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/06
 *     desc   :
 *     version: 1.0
 * </pre>
 */


public interface HttpContants {

//    public static final String BASE_URL = "http://120.77.155.220:8088/base_api/";       //url的基类
    public static final String BASE_URL = "http://112.124.22.238:8081/course_api/";       //url的基类

    public static final String HOME_BANNER_URL = BASE_URL + "banner/query";              //首页轮播图url

    public static final String HOME_CAMPAIGN_URL = BASE_URL + "campaign/recommend";      //首页商品信息url

    public static final String HOT_WARES = BASE_URL + "wares/hot";
    //热卖fragment 数据

    public static final String CATEGORY_LIST = BASE_URL + "category/list";
    //分类一级菜单

    public static final String WARES_LIST = BASE_URL + "wares/list";          //分类二级菜单

    public static final String LOGIN = BASE_URL + "auth/login";                  //登录

    public static final String USER_DETAIL = BASE_URL + "user/get?id=1";

    public static final String REG = BASE_URL + "auth/reg";                  //注册

    public static final String ORDER_CREATE = BASE_URL + "/order/create";        //提交订单

    public static final String requestWeather = "http://apicloud.mob.com/v1/weather/query";
    //mob查询天气的接口

    public static final String WARES_CAMPAIN_LIST = BASE_URL + "wares/campaign/list";
    //热门商品下的商品列表

    public static final String ADDRESS_LIST   = BASE_URL + "addr/list";           //收货地址列表
    public static final String ADDRESS_CREATE = BASE_URL + "addr/create";         //创建新的地址
    public static final String ADDRESS_UPDATE = BASE_URL + "addr/update";          //更新新的地址

    public static final String WARES_DETAIL = BASE_URL + "wares/detail.html";        //商品详情图文详情

    public static final String FAVORITE_LIST   = BASE_URL + "favorite/list";
    public static final String FAVORITE_CREATE = BASE_URL + "favorite/create";


}
