package com.alibaba.tesla.appmanager.server.service.pack;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.container.ComponentPackageTaskMessage;

/**
 * 打包服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface PackService {

    void retryComponentPackageTask(ComponentPackageTaskMessage componentPackageTaskMessageDO);

    void createComponentPackageTask(ComponentPackageTaskMessage componentPackageTaskMessage);

    JSONObject generateBuildOptions(ComponentPackageTaskMessage message);

    JSONObject buildOptions4InternalAddon(String appId, String namespaceId, String stageId, String addonId, Boolean isDevelop);

    JSONObject buildOptions4ResourceAddon(String appId, String namespaceId, String stageId, String addonId, String addonName);

    JSONObject buildOptions4K8sMicroService(String appId, String namespaceId, String stageId, String microServiceId, String branch);

    JSONObject buildOptions4Helm(String appId, String helmPackageId, String branch);
}
