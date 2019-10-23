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

    public static final String BASE_URL = "http://localhost:8094/";       //url的基类

    public static final String HOME_BANNER_URL = BASE_URL + "wx/home/index";              //首页轮播图url

    public static final String HOME_CAMPAIGN_URL = BASE_URL + "wx/goods/list";      //首页商品信息url

    public static final String HOT_WARES = BASE_URL + "wx/goods/list";
    //热卖fragment 数据

    public static final String CATEGORY_LIST = BASE_URL + "wx/goods/category";
    //分类一级菜单

    public static final String WARES_LIST = "http://112.124.22.238:8081/course_api/wares/list";          //分类二级菜单

    public static final String LOGIN = BASE_URL + "wx/auth/login";                  //登录

    public static final String USER_DETAIL = BASE_URL + "wx/auth/info";

    public static final String REG = BASE_URL + "wx/auth/register";                  //注册

    public static final String ORDER_CREATE = BASE_URL + "wx/order/submit";        //提交订单

    public static final String requestWeather = "http://apicloud.mob.com/v1/weather/query";
    //mob查询天气的接口

    public static final String WARES_CAMPAIN_LIST = BASE_URL + "wx/goods/list";
    //热门商品下的商品列表

    public static final String ADDRESS_LIST   = BASE_URL + "wx/address/list";           //收货地址列表
    public static final String ADDRESS_CREATE = BASE_URL + "wx/address/save";         //创建新的地址
    public static final String ADDRESS_UPDATE = BASE_URL + "wx/address/save";          //更新新的地址

    public static final String WARES_DETAIL = "http://112.124.22.238:8081/wares/detail.html";        //商品详情图文详情

    public static final String FAVORITE_LIST   = BASE_URL + "wx/collect/list";
    public static final String FAVORITE_CREATE = BASE_URL + "wx/collect/addordelete";


}
