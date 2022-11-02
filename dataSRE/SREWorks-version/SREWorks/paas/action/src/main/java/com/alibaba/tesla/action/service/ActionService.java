package com.alibaba.tesla.action.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.params.ActionParam;
import com.alibaba.tesla.action.dao.ActionDO;
import com.alibaba.tesla.action.params.QueryParam;
import com.alibaba.tesla.action.params.SaveActionParam;

import javax.servlet.http.Cookie;
import java.util.List;

public interface ActionService {

    List<ActionParam> getActionSimpleList(QueryParam queryParam);

    Integer getCount(QueryParam queryParam);

    ActionParam getAction(Long id);

    int updateStatus(Long id, String status);

    Object callAction(ActionParam actionParam, String env, Cookie[] cookies, String tenant);

    void saveAction(SaveActionParam data);

    JSONObject getData(ActionParam actionParam, String env, Cookie[] cookies, String tenant);

    JSONObject getData(ActionDO actionParam);

    JSONObject getExecData(String actionType, String appCode, JSONObject result);

    void checkBpmsStatus();
}
