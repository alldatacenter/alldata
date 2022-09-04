package com.alibaba.tesla.authproxy.model.vo;

import java.util.List;

/**
 * <p>Description: 初始化应用菜单，前端传输的JSON数据对应的VO对象 <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/5/8 下午3:52
 */
public class AppMenuVO {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 该应用拥有的菜单集合
     */
    private List<MenuVO> menus;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public List<MenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuVO> menus) {
        this.menus = menus;
    }
}