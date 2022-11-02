package com.alibaba.tesla.appmanager.server.service.converter;

import com.alibaba.tesla.appmanager.domain.req.converter.AddParametersToLaunchReq;
import com.alibaba.tesla.appmanager.domain.res.converter.AddParametersToLaunchRes;

/**
 * 转换器服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ConverterService {

    /**
     * 向 launch YAML 中附加全局参数
     *
     * @param request 请求内容
     * @return 返回数据
     */
    AddParametersToLaunchRes addParametersToLaunch(AddParametersToLaunchReq request);
}
