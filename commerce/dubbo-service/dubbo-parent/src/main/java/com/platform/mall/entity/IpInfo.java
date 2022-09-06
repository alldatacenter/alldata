package com.platform.mall.entity;

import java.io.Serializable;

/**
 * @author wulinhao
 */
public class IpInfo implements Serializable{

    String url;

    String p;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }
}
