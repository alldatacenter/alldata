package com.alibaba.tesla.authproxy.model.vo;

import java.util.List;

/**
 * <p>Description:  <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/5/8 下午4:09
 */
public class MenuVO {

    String name;

    String sref;

    String icon;

    String comment;

    String headerTitleSet;

    List<MenuVO> children;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSref() {
        return sref;
    }

    public void setSref(String sref) {
        this.sref = sref;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<MenuVO> getChildren() {
        return children;
    }

    public void setChildren(List<MenuVO> children) {
        this.children = children;
    }

    public String getHeaderTitleSet() {
        return headerTitleSet;
    }

    public void setHeaderTitleSet(String headerTitleSet) {
        this.headerTitleSet = headerTitleSet;
    }
}
