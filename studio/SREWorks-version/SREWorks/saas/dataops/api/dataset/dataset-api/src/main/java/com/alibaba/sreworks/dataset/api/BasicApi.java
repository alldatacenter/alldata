package com.alibaba.sreworks.dataset.api;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface BasicApi {
    /**
     * 普通对象转成JSONObject对象
     * @param obj
     * @return
     */
    default JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }
        return JSONObject.parseObject(JSONObject.toJSONString(obj));
    }

    /**
     * 普通对象转成JSONObject对象列表
     * @param objList
     * @return
     */
    default List<JSONObject> convertToJSONObjects(List<? extends Object> objList) {
        if (objList == null || objList.isEmpty()) {
            return new ArrayList<>();
        }

        return Collections.synchronizedList(objList).parallelStream().map(this::convertToJSONObject).collect(Collectors.toList());
    }
}
