package com.platform.mall.entity;

import java.io.Serializable;
import java.util.List;

/**
 * @author wulinhao
 */
public class IpWeatherResult implements Serializable {

    String msg;

    List<City> result;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<City> getResult() {
        return result;
    }

    public void setResult(List<City> result) {
        this.result = result;
    }
}
