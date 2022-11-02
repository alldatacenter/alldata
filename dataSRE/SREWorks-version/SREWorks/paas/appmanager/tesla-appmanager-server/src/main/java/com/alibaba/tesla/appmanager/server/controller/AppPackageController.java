package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.api.provider.AppPackageTaskProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.NetworkUtil;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.*;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageReleaseRes;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageSyncExternalRes;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageUrlRes;
import com.alibaba.tesla.appmanager.domain.res.apppackage.ApplicationConfigurationGenerateRes;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;

/**
 * App Package 管理
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/apps/{appId}/app-packages")
@RestController
public class AppPackageController extends AppManagerBaseController {

    @Autowired
    private AppPackageTaskProvider appPackageTaskProvider;

    @Autowired
    private AppPackageProvider appPackageProvider;

    @Autowired
    private AppPackageTagService appPackageTagService;

    /**
     * @api {get} /apps/:appId/app-packages 获取应用包列表
     * @apiName GetApplicationPackageList
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (GET Parameters) {Number} id 过滤条件：应用包 ID
     * @apiParam (GET Parameters) {String} packageVersion 过滤条件：包版本号
     * @apiParam (GET Parameters) {String[]} tagList 过滤条件：标签列表
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     */
    @GetMapping
    public TeslaBaseResult list(
            @PathVariable String appId, @ModelAttribute AppPackageQueryReq request,
            OAuth2Authentication auth) {
        request.setAppId(appId);
        return buildSucceedResult(appPackageProvider.list(request, getOperator(auth)));
    }

    /**
     * @api {post} /apps/:appId/app-packages 创建应用包（直接关联）
     * @apiName PostApplicationPackage
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (JSON Body) {String} version 版本号
     * @apiParam (JSON Body) {Number[]} componentPackageIdList 组件包 ID 列表
     */
    @PostMapping
    @ResponseBody
    public TeslaBaseResult create(
            @PathVariable String appId, @RequestBody AppPackageCreateReq request,
            OAuth2Authentication auth) {
        request.setAppId(appId);
        AppPackageDTO appPackageDTO = appPackageProvider.create(request, getOperator(auth));
        return buildSucceedResult(appPackageDTO);
    }

    @GetMapping("/{appPackageId}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable String appId, @PathVariable Long appPackageId,
            OAuth2Authentication auth) {
        AppPackageQueryReq request = AppPackageQueryReq.builder()
                .id(appPackageId)
                .withBlobs(true)
                .build();
        AppPackageDTO record = appPackageProvider.get(request, getOperator(auth));
        if (record == null) {
            return buildClientErrorResult("cannot find app package by request");
        }
        return buildSucceedResult(record);
    }

    /**
     * @api {get} /apps/:appId/app-packages/:appPackageId/launch-yaml 生成应用包的默认部署 yaml
     * @apiName GetApplicationPackageLaunchYaml
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @GetMapping("/{appPackageId}/launch-yaml")
    @ResponseBody
    public TeslaBaseResult launchYaml(
            @PathVariable String appId,
            @PathVariable Long appPackageId,
            @ModelAttribute AppPackageGetLaunchYamlReq request,
            @RequestHeader(value = "X-Biz-App") String headerBizApp,
            OAuth2Authentication auth) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        ApplicationConfigurationGenerateRes result = appPackageProvider.generate(
                ApplicationConfigurationGenerateReq.builder()
                        .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                        .appId(appId)
                        .appPackageId(appPackageId)
                        .appInstanceName(request.getAppInstanceName())
                        .unitId(request.getUnit())
                        .clusterId(request.getClusterId())
                        .namespaceId(request.getNamespaceId())
                        .stageId(request.getStageId())
                        .componentPackageConfigurationFirst(request.isComponentPackageConfigurationFirst())
                        .isolateNamespaceId(container.getNamespaceId())
                        .isolateStageId(container.getStageId())
                        .build());
        return buildSucceedResult(result);
    }

    /**
     * @api {post} /apps/:appId/app-packages/import 导入应用包
     * @apiName PostApplicationPackageImport
     * @apiGroup 应用包 API
     * @apiDescription BODY 为实际应用包的二进制流
     * @apiHeader {String} Content-Type application/octet-stream
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Query Parameters) {String} packageVersion 导入后包的实际版本号（覆盖原始包中的版本）
     * @apiParam (Query Parameters) {Boolean} force="true" 是否强制覆盖已有同版本包
     */
    @RequestMapping(value = "import", method = RequestMethod.POST, consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public TeslaBaseResult importPackage(
            @PathVariable String appId,
            @Valid @ModelAttribute AppPackageImportReq request,
            RequestEntity<InputStream> entity,
            OAuth2Authentication auth) {
        request.setAppId(appId.replace("..", ""));
        request.setPackageCreator(getOperator(auth));
        if (request.getForce() == null) {
            request.setForce(true);
        }
        if (request.getResetVersion() == null) {
            request.setResetVersion(false);
        }
        AppPackageDTO appPackageDTO = appPackageProvider.importPackage(request, entity.getBody(), getOperator(auth));
        return buildSucceedResult(appPackageDTO);
    }

    /**
     * @api {get} /apps/:appId/app-packages/latest-version 获取指定应用的应用包最新版本
     * @apiName GetApplicationPackageLatestVersion
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     */
    @GetMapping("/latest-version")
    public TeslaBaseResult latestVersion(@PathVariable String appId, OAuth2Authentication auth) {
        AppPackageTaskNextLatestVersionReq request = AppPackageTaskNextLatestVersionReq.builder()
                .appId(appId)
                .build();
        return buildSucceedResult(appPackageTaskProvider.nextLatestVersion(request, getOperator(auth)));
    }

    /**
     * @api {delete} /apps/:appId/app-packages/:appPackageId 删除指定应用包
     * @apiName DeleteApplicationPackage
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @DeleteMapping("/{appPackageId}")
    public TeslaBaseResult delete(
            @PathVariable String appId, @PathVariable Long appPackageId,
            OAuth2Authentication auth) {
        appPackageProvider.delete(appPackageId, getOperator(auth));
        return buildSucceedResult(Boolean.TRUE);
    }

    /**
     * @api {post} /apps/:appId/app-packages/:appPackageId/on-sale 上架应用包
     * @apiName PostApplicationPackageOnSale
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @PostMapping("/{appPackageId}/on-sale")
    @ResponseBody
    public TeslaBaseResult putOnSale(
            @PathVariable String appId, @PathVariable Long appPackageId,
            HttpServletRequest r, OAuth2Authentication auth) {
        AppPackageTagDO appPackageTagDO = AppPackageTagDO.builder()
                .appPackageId(appPackageId)
                .appId(appId)
                .tag(DefaultConstant.ON_SALE)
                .build();
        appPackageTagService.insert(appPackageTagDO);
        return buildSucceedResult(appPackageTagDO);
    }

    /**
     * @api {delete} /apps/:appId/app-packages/:appPackageId/on-sale 下架应用包
     * @apiName DeleteApplicationPackageOnSale
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @DeleteMapping("/{appPackageId}/on-sale")
    @ResponseBody
    public TeslaBaseResult deleteOnSale(
            @PathVariable String appId, @PathVariable Long appPackageId,
            HttpServletRequest r, OAuth2Authentication auth) {
        AppPackageTagQueryCondition condition = AppPackageTagQueryCondition.builder()
                .appId(appId)
                .appPackageId(appPackageId)
                .tagList(Collections.singletonList(DefaultConstant.ON_SALE))
                .build();
        int count = appPackageTagService.delete(condition);
        return buildSucceedResult(count);
    }

    /**
     * @api {get} /apps/:appId/app-packages/:appPackageId/url 获取应用包下载链接
     * @apiName GetApplicationPackageUrl
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @GetMapping(value = "/{appPackageId}/url")
    @ResponseBody
    public TeslaBaseResult url(
            @PathVariable String appId,
            @PathVariable("appPackageId") Long appPackageId,
            OAuth2Authentication auth) {
        AppPackageUrlRes response = appPackageProvider.generateUrl(appPackageId, getOperator(auth));
        return buildSucceedResult(response);
    }

    /**
     * @api {get} /apps/:appId/app-packages/:appPackageId/download 应用包直接下载
     * @apiName GetApplicationPackageDownload
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     */
    @GetMapping(value = "/{appPackageId}/download")
    @ResponseBody
    public ResponseEntity<Resource> download(
            @PathVariable String appId,
            @PathVariable("appPackageId") Long appPackageId,
            OAuth2Authentication auth) throws IOException {
        AppPackageUrlRes appPackageUrl = appPackageProvider.generateUrl(appPackageId, getOperator(auth));
        File zipFile = Files.createTempFile("download", ".zip").toFile();
        NetworkUtil.download(appPackageUrl.getUrl(), zipFile.getAbsolutePath());
        zipFile.deleteOnExit();
        InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + appId + "-" + appPackageUrl.getFilename());
        return ResponseEntity.ok()
                .headers(header)
                .contentLength(zipFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * @api {post} /apps/:appId/app-packages/:appPackageId/sync 同步指定应用包到外部环境
     * @apiName PostApplicationPackageSync
     * @apiGroup 应用包 API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} appPackageId 应用包 ID
     * @apiParam (JSON Body) {Number} appPackageId 应用包 ID
     * @apiParam (JSON Body) {String} proxyIp 代理 IP 地址
     * @apiParam (JSON Body) {String} proxyPort 代理端口
     * @apiParam (JSON Body) {String} targetEndpoint 目标 Endpoint
     */
    @PostMapping(value = "{appPackageId}/sync")
    @ResponseBody
    public TeslaBaseResult sync(
            @PathVariable String appId,
            @PathVariable("appPackageId") Long appPackageId,
            @RequestBody AppPackageSyncExternalReq request,
            OAuth2Authentication auth) throws IOException, URISyntaxException {
        request.setAppPackageId(appPackageId);
        AppPackageSyncExternalRes response = appPackageProvider.syncExternal(request, getOperator(auth));
        return buildSucceedResult(response);
    }

    @PostMapping(value = "{appPackageId}/release-as-custom")
    @ResponseBody
    public TeslaBaseResult releaseAsCustomAddon(
            @PathVariable String appId,
            @PathVariable("appPackageId") Long appPackageId,
            @RequestBody AppPackageReleaseReq request,
            HttpServletRequest r, OAuth2Authentication auth) {
        request.setAppPackageId(appPackageId);
        AppPackageReleaseRes appPackageReleaseRes = appPackageProvider.releaseAsCustomAddon(request);
        return buildSucceedResult(appPackageReleaseRes);
    }
}
