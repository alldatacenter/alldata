package com.alibaba.tesla.appmanager.server.dynamicscript.handler;

import com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance.RtComponentInstanceGetStatusReq;
import com.alibaba.tesla.appmanager.domain.res.rtcomponentinstance.RtComponentInstanceGetStatusRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * Component Watch Cron Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentWatchCronHandler extends GroovyHandler {

    /**
     * 获取指定组件实例的当前状态信息
     *
     * @return 当前该组件实例状态
     */
    RtComponentInstanceGetStatusRes get(RtComponentInstanceGetStatusReq request);
}
