package com.alibaba.tesla.authproxy.model;

import java.io.Serializable;

/**
 * <p>Description: 菜单DO<／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class MenuDO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String appId;
    private String menuCode;
    private String menuName;
    private Integer isLeaf;
    private String parentCode;
    private String icon;
    private Integer idx;
    private String menuTitle;
    private String menuUrl;
    private Integer isEnable;
    private String headerTitleSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? null : appId.trim();
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode == null ? null : menuCode.trim();
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName == null ? null : menuName.trim();
    }

    public Integer getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Integer isLeaf) {
        this.isLeaf = isLeaf;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode == null ? null : parentCode.trim();
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon == null ? null : icon.trim();
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public void setMenuTitle(String menuTitle) {
        this.menuTitle = menuTitle == null ? null : menuTitle.trim();
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }

    public String getMenuUrl() {
        return menuUrl;
    }

    public void setMenuUrl(String menuUrl) {
        this.menuUrl = menuUrl == null ? null : menuUrl.trim();
    }

    public Integer getIsEnable() {
        return isEnable;
    }

    public void setIsEnable(Integer isEnable) {
        this.isEnable = isEnable;
    }

    public String getHeaderTitleSet() {
        return headerTitleSet;
    }

    public void setHeaderTitleSet(String headerTitleSet) {
        this.headerTitleSet = headerTitleSet;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        MenuDO other = (MenuDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getAppId() == null ? other.getAppId() == null : this.getAppId().equals(other.getAppId()))
                && (this.getMenuCode() == null ? other.getMenuCode() == null : this.getMenuCode().equals(other.getMenuCode()))
                && (this.getMenuName() == null ? other.getMenuName() == null : this.getMenuName().equals(other.getMenuName()))
                && (this.getIsLeaf() == null ? other.getIsLeaf() == null : this.getIsLeaf().equals(other.getIsLeaf()))
                && (this.getParentCode() == null ? other.getParentCode() == null : this.getParentCode().equals(other.getParentCode()))
                && (this.getIcon() == null ? other.getIcon() == null : this.getIcon().equals(other.getIcon()))
                && (this.getIdx() == null ? other.getIdx() == null : this.getIdx().equals(other.getIdx()))
                && (this.getMenuTitle() == null ? other.getMenuTitle() == null : this.getMenuTitle().equals(other.getMenuTitle()))
                && (this.getMenuUrl() == null ? other.getMenuUrl() == null : this.getMenuUrl().equals(other.getMenuUrl()))
                && (this.getHeaderTitleSet() == null ? other.getHeaderTitleSet() == null : this.getHeaderTitleSet().equals(other.getHeaderTitleSet()))
                && (this.getIsEnable() == null ? other.getIsEnable() == null : this.getIsEnable().equals(other.getIsEnable()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAppId() == null) ? 0 : getAppId().hashCode());
        result = prime * result + ((getMenuCode() == null) ? 0 : getMenuCode().hashCode());
        result = prime * result + ((getMenuName() == null) ? 0 : getMenuName().hashCode());
        result = prime * result + ((getIsLeaf() == null) ? 0 : getIsLeaf().hashCode());
        result = prime * result + ((getParentCode() == null) ? 0 : getParentCode().hashCode());
        result = prime * result + ((getIcon() == null) ? 0 : getIcon().hashCode());
        result = prime * result + ((getIdx() == null) ? 0 : getIdx().hashCode());
        result = prime * result + ((getMenuTitle() == null) ? 0 : getMenuTitle().hashCode());
        result = prime * result + ((getMenuUrl() == null) ? 0 : getMenuUrl().hashCode());
        result = prime * result + ((getHeaderTitleSet() == null) ? 0 : getHeaderTitleSet().hashCode());
        result = prime * result + ((getIsEnable() == null) ? 0 : getIsEnable().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", appId=").append(appId);
        sb.append(", menuCode=").append(menuCode);
        sb.append(", menuName=").append(menuName);
        sb.append(", isLeaf=").append(isLeaf);
        sb.append(", parentCode=").append(parentCode);
        sb.append(", icon=").append(icon);
        sb.append(", idx=").append(idx);
        sb.append(", menuTitle=").append(menuTitle);
        sb.append(", menuUrl=").append(menuUrl);
        sb.append(", headerTitleSet=").append(headerTitleSet);
        sb.append(", isEnable=").append(isEnable);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}