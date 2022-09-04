package com.alibaba.sreworks.dataset.services.inter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.InterfaceConfigService;
import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.exception.InterfaceNotExistException;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceRequestParam;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfig;
import com.alibaba.sreworks.dataset.processors.ESProcessor;
import com.alibaba.sreworks.dataset.processors.MysqlProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据接口Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 19:47
 */

@Service
public class InterfaceServiceImpl extends AbstractInterfaceService {

    @Autowired
    InterfaceConfigService interfaceConfigService;

    @Autowired
    ESProcessor esProcessor;

    @Autowired
    MysqlProcessor mysqlProcessor;

    @Override
    public Map<String, Object> get(String name, Map<String, Object> params) throws Exception {
        JSONObject result = interfaceConfigService.getConfigByName(name);
        if (CollectionUtils.isEmpty(result)) {
            throw new InterfaceNotExistException(String.format("数据接口[%s]不存在,请先定义接口", name));
        }

        InterfaceConfig interfaceConfig = JSONObject.toJavaObject(result, InterfaceConfig.class);
        List<InterfaceRequestParam> requestParams = JSONArray.parseArray(interfaceConfig.getRequestParams(), InterfaceRequestParam.class);

        Map<String, Object> validParams = checkParams(requestParams, params, interfaceConfig.getPaging());

        String source = interfaceConfig.getDataSourceType();
        if (source.equals(ValidConstant.ES_SOURCE)) {
            return esProcessor.getModelData(interfaceConfig, validParams);
        } else if (source.equals(ValidConstant.MYSQL_SOURCE)) {
            return mysqlProcessor.getModelData(interfaceConfig, validParams);
        }

        return null;
    }

    @Override
    public Map<String, Object> get(String name, Map<String, Object> params, JSONObject body) throws Exception {
        if (body != null) {
            params.putAll(body);
        }
        return get(name, params);
    }
}
