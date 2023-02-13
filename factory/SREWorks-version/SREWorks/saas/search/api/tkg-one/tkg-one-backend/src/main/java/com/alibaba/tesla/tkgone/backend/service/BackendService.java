package com.alibaba.tesla.tkgone.backend.service;

import java.security.InvalidParameterException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.backend.service.dto.BackendDTO;

import org.springframework.stereotype.Service;

/**
 * 存储后端配置service
 * @author xueyong.zxy
 */
@Service
public class BackendService {
    public static final JSONArray BACKEND_TYPES = JSONArray.parseArray("[\n" +
            "    {\n" +
            "        \"type\":\"ZSearch\",\n" +
            "        \"name\":\"蚂蚁ZSearch\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"type\":\"ElasticSearch\",\n" +
            "        \"name\":\"ElasticSearch\"\n" +
            "    }\n" +
            "]");

    private boolean isBackendTypeExist(String backendType) {
        for (JSONObject backendTypeObject : BackendService.BACKEND_TYPES.toJavaList(JSONObject.class)) {
            if (backendTypeObject.getString("type").equals(backendType)) {
                return true;
            }
        }
        return false;
    }

    public BackendDTO addBackend(BackendDTO backendDTO) throws InvalidParameterException {
        if (!isBackendTypeExist(backendDTO.getType())) {
            throw new InvalidParameterException("backend type not exist!");
        }

        return null;
    }
}
