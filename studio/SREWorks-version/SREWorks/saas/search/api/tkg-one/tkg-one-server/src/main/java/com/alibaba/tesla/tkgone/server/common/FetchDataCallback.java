package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author junwei.jjw@alibaba-inc.com
 * @date 2019/10/27
 */
public interface FetchDataCallback {
    void fetch(List<JSONObject> retList);
}
