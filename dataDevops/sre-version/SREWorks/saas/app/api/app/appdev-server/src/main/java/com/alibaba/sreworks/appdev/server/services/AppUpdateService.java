package com.alibaba.sreworks.appdev.server.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppUpdateService extends AbstractAppDeployService {

    private void patchNamespace(AppInstance appInstance) throws IOException, ApiException {
        CoreV1Api api = api(appInstance);
        JSONArray patchArray = JsonUtil.list(JsonUtil.map(
            "op", "replace",
            "path", "/metadata/labels/appInstanceId",
            "value", appInstance.getId().toString()
        ));
        V1Patch patch = new V1Patch(JSONObject.toJSONString(patchArray));
        api.patchNamespace(appInstance.namespace(), patch, null, null, null, null);
    }

    public void replaceResourceQuota(AppInstance appInstance) throws IOException, ApiException {
        CoreV1Api api = api(appInstance);
        api.replaceNamespacedResourceQuota("mem-cpu", appInstance.namespace(), getResourceQuota(appInstance),
            null, null, null);
    }

    void flushAppComponentInstanceList(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList) {

        List<AppComponentInstance> nowAppComponentInstanceList = appComponentInstanceRepository
            .findAllByAppInstanceId(appInstance.getId());
        Map<String, AppComponentInstance> nowAppComponentInstanceMap = nowAppComponentInstanceList.stream()
            .collect(Collectors.toMap(AppComponentInstance::getName, x -> x));
        List<String> nameList = appComponentInstanceList.stream()
            .map(AppComponentInstance::getName).collect(Collectors.toList());

        for (AppComponentInstance appComponentInstance : appComponentInstanceList) {
            AppComponentInstance nowAppComponentInstance = nowAppComponentInstanceMap.get(
                appComponentInstance.getName());
            if (nowAppComponentInstance != null) {
                appComponentInstance.setId(nowAppComponentInstance.getId());
            }
        }
        appComponentInstanceRepository.saveAll(appComponentInstanceList);
        appComponentInstanceRepository.flush();
        for (AppComponentInstance nowAppComponentInstance : nowAppComponentInstanceList) {
            if (!nameList.contains(nowAppComponentInstance.getName())) {
                appComponentInstanceRepository.deleteById(nowAppComponentInstance.getId());
            }
        }
    }

    public void update(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList, String user)
        throws IOException, ApiException {
        patchNamespace(appInstance);
        replaceResourceQuota(appInstance);
        flushAppComponentInstanceList(appInstance, appComponentInstanceList);
        run(appInstance, appComponentInstanceList);
        appInstance.setLastModifier(user);
        appInstance.setGmtModified(System.currentTimeMillis() / 1000);
    }

}
