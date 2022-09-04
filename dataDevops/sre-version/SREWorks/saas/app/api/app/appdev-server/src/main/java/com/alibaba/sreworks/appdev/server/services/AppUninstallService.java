package com.alibaba.sreworks.appdev.server.services;

import java.io.IOException;

import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.flyadmin.server.services.MicroServiceService;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppUninstallService extends AbstractAppDeployService {

    @Autowired
    MicroServiceService microServiceService;

    private void deleteNamespace(AppInstance appInstance) throws IOException, ApiException {
        CoreV1Api api = api(appInstance);
        api.deleteNamespaceAsync(appInstance.namespace(), null, null, null, null, null, null, null);
    }

    public void uninstall(AppInstance appInstance) throws IOException, ApiException {
        deleteNamespace(appInstance);
    }

    public void uninstallComponent(
        AppInstance appInstance, AppComponentInstance appComponentInstance) throws IOException {
        microServiceService.delete(appInstance, appComponentInstance);
        appComponentInstanceRepository.deleteById(appComponentInstance.getId());
    }

}
