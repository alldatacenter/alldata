package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.req.flowmanager.FlowManagerSyncExternalReq;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Flow 管理服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface FlowManagerProvider {

    void upgrade(InputStream inputStream, String operator);

//    void syncToExternal(FlowManagerSyncExternalReq req, String operator) throws IOException, URISyntaxException;
}
