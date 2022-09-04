package com.alibaba.tesla.appmanager.server.dynamicscript.handler;

import com.alibaba.tesla.appmanager.domain.req.destroy.DestroyComponentInstanceReq;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * 组件销毁接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentDestroyHandler extends GroovyHandler {

    /**
     * 销毁组件实例
     *
     * @param request 销毁请求
     */
    void destroy(DestroyComponentInstanceReq request);
}
