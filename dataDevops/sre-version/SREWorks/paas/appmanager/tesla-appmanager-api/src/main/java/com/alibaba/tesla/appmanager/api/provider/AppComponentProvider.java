package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.dto.AppComponentDTO;
import com.alibaba.tesla.appmanager.domain.req.appcomponent.AppComponentQueryReq;

import java.util.List;

/**
 * 应用关联组件 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface AppComponentProvider {

    /**
     * 获取指定 appId 下的所有关联 Component 对象
     *
     * @param request 查询请求
     * @param operator 操作人
     * @return List of AppComponentDTO
     */
    List<AppComponentDTO> list(AppComponentQueryReq request, String operator);
}
