package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppPackageTaskProvider;
import com.alibaba.tesla.appmanager.api.provider.ProductReleaseProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.req.productrelease.CreateAppPackageTaskInProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageTaskCreateRes;
import com.alibaba.tesla.appmanager.domain.res.productrelease.CreateAppPackageTaskInProductReleaseTaskRes;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseTaskAppPackageTaskRelRepository;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;
import com.alibaba.tesla.appmanager.server.service.productrelease.ProductReleaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 产品与发布版本 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class ProductReleaseProviderImpl implements ProductReleaseProvider {

    @Autowired
    private AppPackageTaskProvider appPackageTaskProvider;

    @Autowired
    private ProductReleaseService productReleaseService;

    @Autowired
    private GitService gitService;

    @Autowired
    private ProductReleaseTaskAppPackageTaskRelRepository productReleaseTaskAppPackageTaskRelRepository;

    /**
     * 获取指定 productId + releaseId + appId 的 launch yaml 文件内容
     *
     * @param productId 产品 ID
     * @param releaseId 发布版本 ID
     * @param appId     应用 ID
     * @return launch yaml 文件内容
     */
    @Override
    public String getLaunchYaml(String productId, String releaseId, String appId) {
        return productReleaseService.getLaunchYaml(productId, releaseId, appId);
    }

    /**
     * 在产品发布版本任务中创建 AppPackage 打包任务
     *
     * @param request 请求内容
     * @return 应用包任务 ID
     */
    @Override
    public CreateAppPackageTaskInProductReleaseTaskRes createAppPackageTaskInProductReleaseTask(
            CreateAppPackageTaskInProductReleaseTaskReq request) {
        // Git 切换到自己的分支
        gitService.checkoutBranch(request.getLogContent(), request.getBranch(), request.getDir());
        log.info("git repo has checkout to branch {}|dir={}|productId={}|releaseId={}",
                request.getBranch(), request.getDir().toString(), request.getProductId(), request.getReleaseId());

        // 创建应用包任务
        AppPackageTaskCreateRes res = appPackageTaskProvider.create(
                AppPackageTaskCreateReq.builder()
                        .appId(request.getAppId())
                        .tags(Collections.singletonList(request.getTag()))
                        .components(buildYamlToComponentBinderList(request.getDir(), request.getBuildPath()))
                        .build(),
                DefaultConstant.SYSTEM_OPERATOR);
        Long appPackageTaskId = res.getAppPackageTaskId();

        // 插入关联数据
        ProductReleaseTaskAppPackageTaskRelDO appPackageTaskRelDO = ProductReleaseTaskAppPackageTaskRelDO.builder()
                .taskId(request.getTaskId())
                .appPackageTaskId(appPackageTaskId)
                .build();
        int count = productReleaseTaskAppPackageTaskRelRepository.insert(appPackageTaskRelDO);
        if (count == 0) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "cannot insert product release task app package task " +
                    "rel, count=0|task={}", JSONObject.toJSONString(appPackageTaskRelDO));
        }
        log.info("product release task app package task rel has created|taskId={}|appPackageTaskId={}|request={}",
                request.getTaskId(), appPackageTaskId, JSONObject.toJSONString(request));

        return CreateAppPackageTaskInProductReleaseTaskRes.builder()
                .appPackageTaskId(appPackageTaskId)
                .build();
    }

    /**
     * 将指定的 build.yaml 文件转换为内部的 ComponentBinder List 对象
     *
     * @param dir       Git Clone 的本地目录
     * @param buildPath 构建文件 Yaml 路径
     * @return List of ComponentBinder
     */
    private List<ComponentBinder> buildYamlToComponentBinderList(Path dir, String buildPath) {
        Yaml yaml = SchemaUtil.createYaml(Arrays.asList(Iterable.class, Object.class));
        Path actualPath = dir.resolve(buildPath);
        Iterable<Object> iterable;
        try {
            iterable = yaml.loadAll(new String(Files.readAllBytes(actualPath)));
        } catch (IOException e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("load build path failed, path=%s", actualPath));
        }

        List<ComponentBinder> results = new ArrayList<>();
        for (Object object : iterable) {
            JSONObject root = new JSONObject((Map) object);
            JSONObject options = root.getJSONObject("options");
            String componentName = root.getString("componentName");
            ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(root.getString("componentType"));
            results.add(ComponentBinder.builder()
                    .componentType(componentType)
                    .componentName(componentName)
                    .useRawOptions(true)
                    .options(options)
                    .build());
        }
        return results;
    }
}
