package com.alibaba.sreworks.dataset.services.inter;

import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.dataset.api.inter.InterfaceService;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.common.type.ColumnType;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceRequestParam;
import com.alibaba.sreworks.dataset.processors.AbstractProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽象数据接口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 19:29
 */
public abstract class AbstractInterfaceService implements InterfaceService {

    /**
     * 请求参数数值校验,自动去除null值参数
     * @param interfaceParams
     * @param requestParams
     * @return
     * @throws Exception
     */
    protected Map<String, Object> checkParams(List<InterfaceRequestParam> interfaceParams, Map<String, Object> requestParams, boolean paging) throws Exception {
        Map<String, Object> validParams = new HashMap<>();
        if (!CollectionUtils.isEmpty(interfaceParams)) {
            for (InterfaceRequestParam interfaceParam : interfaceParams) {
                String name = interfaceParam.getName();
                Boolean required = interfaceParam.getRequired();
                String defaultValue = interfaceParam.getDefaultValue();
                ColumnType paramType = ColumnType.valueOf(interfaceParam.getType());

                Object requestValue = requestParams.getOrDefault(name, null);
                Object validValue = getColumnValue(name, requestValue, paramType, required, defaultValue);
                if (validValue != null) {
                    validParams.put(name, validValue);
                }
            }
        }

        if (paging) {
            if (requestParams.get(AbstractProcessor.PAGE_NUM_KEY) != null) {
                validParams.put(AbstractProcessor.PAGE_NUM_KEY, requestParams.get(AbstractProcessor.PAGE_NUM_KEY));
            }
            if (requestParams.get(AbstractProcessor.PAGE_SIZE_KEY) != null) {
                validParams.put(AbstractProcessor.PAGE_SIZE_KEY, requestParams.get(AbstractProcessor.PAGE_SIZE_KEY));
            }
        }

        return validParams;
    }

    /**
     * 请求参数数值类型强制转换
     * @param paramLabel
     * @param requestValue
     * @param type
     * @param required
     * @param defaultValue
     * @return
     * @throws Exception
     */
    private Object getColumnValue(String paramLabel, Object requestValue, ColumnType type, Boolean required, Object defaultValue) throws Exception {
        Object object;
        Object defaultObject;

        String strObj = TypeUtils.castToString(requestValue);
        if (StringUtils.isEmpty(strObj)) {
            requestValue = null;
        }

        try {
            switch (type) {
                case BOOLEAN:
                    object = TypeUtils.castToBoolean(requestValue);
                    defaultObject = TypeUtils.castToBoolean(defaultValue);
                    break;
                case INT:
                    object = TypeUtils.castToInt(requestValue);
                    defaultObject = TypeUtils.castToInt(defaultValue);
                    break;
                case LONG:
                    object = TypeUtils.castToLong(requestValue);
                    defaultObject = TypeUtils.castToLong(defaultValue);
                    break;
                case FLOAT:
                    object = TypeUtils.castToFloat(requestValue);
                    defaultObject = TypeUtils.castToFloat(defaultValue);
                    break;
                case DOUBLE:
                    object = TypeUtils.castToDouble(requestValue);
                    defaultObject = TypeUtils.castToDouble(defaultValue);
                    break;
                case STRING:
                    object = TypeUtils.castToString(requestValue);
                    defaultObject = TypeUtils.castToString(defaultValue);
                    break;
                default:
                    throw new ParamException(String.format("请求参数[%s]类型[%s]非法, 请仔细核对接口定义", paramLabel, type));
            }
        } catch (Exception e) {
            throw new ParamException(String.format("请求参数[%s]与类型[%s]不匹配, 请仔细核对接口定义", paramLabel, type));
        }

        if (object == null) {
            if (required && defaultObject == null) {
                throw new ParamException(String.format("请求参数[%s]不能为空, 请仔细核对接口定义", paramLabel));
            }
            object = defaultObject;
        }

        return object;
    }
}
