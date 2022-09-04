package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 云账号列表获取 API 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountListParam implements Serializable {

    public static final long serialVersionUID = 1L;

    private String search = "";

    public void cleanSelf() {
        this.search = search.trim();
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
