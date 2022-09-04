package com.alibaba.tesla.authproxy.model.vo;

import java.util.List;

/**
 * <p>Title: MenuDoTreeVO.java<／p>
 * <p>Description: 菜单树形结构数据值对象 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年4月9日
 */
public class MenuDoTreeVO {

    private long id;

    private String code;

    private String name;

    private boolean isLeaf;

    private String icon;

    private String sref;

    private String title;

    private String headerTitleSet;

    private List<MenuDoTreeVO> children;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public String getSref() {
        return sref;
    }

    public void setSref(String sref) {
        this.sref = sref;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MenuDoTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuDoTreeVO> children) {
        this.children = children;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getHeaderTitleSet() {
        return headerTitleSet;
    }

    public void setHeaderTitleSet(String headerTitleSet) {
        this.headerTitleSet = headerTitleSet;
    }
}
