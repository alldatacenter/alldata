package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AddonMetaProvider;
import com.alibaba.tesla.appmanager.api.provider.AppAddonProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.dto.AddonMetaDTO;
import com.alibaba.tesla.appmanager.domain.dto.AppAddonDTO;
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonQueryReq;
import com.alibaba.tesla.appmanager.domain.req.AppAddonUpdateReq;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Objects;

/**
 * 应用 Addon 接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@RequestMapping("/apps/{appId}/addon")
@RestController
@Slf4j
public class AppAddonController extends AppManagerBaseController {

    @Autowired
    private AppAddonProvider appAddonProvider;

    @Autowired
    private AddonMetaProvider addonMetaProvider;

    /**
     * @api {get} /apps/:appId/addon 获取应用下 Addon 列表
     * @apiName GetApplicationAddonList
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (GET Parameters) {String[]} addonTypeList 过滤条件：Addon 类型列表 (可选 MICROSERVICE, K8S_MICROSERVICE, K8S_JOB, RESOURCE_ADDON, INTERNAL_ADDON, TRAIT_ADDON, CUSTOM_ADDON, ABM_CHART)
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     */
    @GetMapping
    @ResponseBody
    public TeslaBaseResult list(
            @PathVariable String appId,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            @ModelAttribute AppAddonQueryReq request) throws Exception {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        request.setNamespaceId(container.getNamespaceId());
        request.setStageId(container.getStageId());
        request.setAppId(appId);
        Pagination<AppAddonDTO> addonList = appAddonProvider.list(request);
        Pagination<JSONObject> result = Pagination.transform(addonList, appAddonDTO -> {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(appAddonDTO);
            AddonMetaDTO addonMetaDTO = addonMetaProvider.get(appAddonDTO.getAddonId(),
                    appAddonDTO.getAddonVersion());
            if (Objects.nonNull(addonMetaDTO)) {
                jsonObject.put("componentsSchema", addonMetaDTO.getComponentsSchema());
            }
            return jsonObject;
        });
        return buildSucceedResult(result);
    }

    /**
     * @api {post} /apps/:appId/addon 新增应用下单个 Addon 绑定
     * @apiName PostApplicationAddon
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (JSON Body) {Number} addonMetaId Addon 元数据 ID
     * @apiParam (JSON Body) {String} addonName Addon Name (当前应用下的唯一标识)
     * @apiParam (JSON Body) {Object} spec Addon 配置信息
     */
    @PostMapping
    @ResponseBody
    public TeslaBaseResult create(
            @PathVariable String appId,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            @RequestBody AppAddonCreateReq request) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        request.setNamespaceId(container.getNamespaceId());
        request.setStageId(container.getStageId());
        request.setAppId(appId);
        try {
            boolean result = appAddonProvider.create(request);
            return buildSucceedResult(result);
        } catch (DuplicateKeyException e) {
            return buildResult(AppErrorCode.FORBIDDEN_ADDON_NAME_EXISTS.getCode(),
                    AppErrorCode.FORBIDDEN_ADDON_NAME_EXISTS.getDescription(), null);
        }
    }

    /**
     * @api {get} /apps/:appId/addon/:addonName 获取应用下单个 Addon 详情
     * @apiName GetApplicationAddon
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} addonName Addon Name (当前应用下的唯一标识)
     */
    @GetMapping(value = "/{addonName}")
    @ResponseBody
    public TeslaBaseResult get(
            @PathVariable String appId,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            @PathVariable String addonName) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        AppAddonDTO appAddonDTO = appAddonProvider.get(appId, namespaceId, stageId, addonName);
        AddonMetaDTO addonMetaDTO = addonMetaProvider.get(appAddonDTO.getAddonId(), appAddonDTO.getAddonVersion());
        JSONObject result = (JSONObject) JSON.toJSON(appAddonDTO);
        result.put("componentsSchema", addonMetaDTO.getComponentsSchema());
        return buildSucceedResult(result);
    }

    /**
     * @api {put} /apps/:appId/addon/:addonName 更新应用下单个 Addon 绑定配置
     * @apiName PutApplicationAddon
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} addonName Addon Name (当前应用下的唯一标识)
     * @apiParam (JSON Body) {Object} spec Addon 配置信息
     */
    @PutMapping(value = "/{addonName}")
    @ResponseBody
    public TeslaBaseResult update(
            @PathVariable String appId,
            @PathVariable String addonName,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp,
            @RequestBody AppAddonUpdateReq request) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        request.setAppId(appId);
        request.setNamespaceId(namespaceId);
        request.setStageId(stageId);
        request.setAddonName(addonName);
        try {
            boolean result = appAddonProvider.update(request);
            return buildSucceedResult(result);
        } catch (DuplicateKeyException e) {
            return buildResult(AppErrorCode.FORBIDDEN_ADDON_NAME_EXISTS.getCode(),
                    AppErrorCode.FORBIDDEN_ADDON_NAME_EXISTS.getDescription(), null);
        }
    }

    /**
     * @api {delete} /apps/:appId/addon/:addonName 删除应用下单个 Addon
     * @apiName DeleteApplicationAddon
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} addonName Addon Name (当前应用下的唯一标识)
     */
    @DeleteMapping(value = "/{addonName}")
    @ResponseBody
    public TeslaBaseResult delete(
            @PathVariable String appId,
            @PathVariable String addonName,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(addonName)) {
            return buildSucceedResult(Boolean.TRUE);
        }
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        boolean result = appAddonProvider.delete(appId, namespaceId, stageId, addonName);
        return buildSucceedResult(result);
    }

    /**
     * @api {get} /apps/:appId/addon/:addonName/data-outputs 获取应用下单个 Addon 的 DataOutputs
     * @apiName GetApplicationAddonDataOutputs
     * @apiGroup 应用 Addon API
     * @apiParam (Path Parameters) {String} appId 应用 ID
     * @apiParam (Path Parameters) {String} addonName Addon Name (当前应用下的唯一标识)
     */
    @GetMapping(value = "/{addonName}/data-outputs")
    @ResponseBody
    public TeslaBaseResult listDataOutputs(
            @PathVariable String appId,
            @PathVariable String addonName,
            @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        AppAddonDTO appAddonDTO = appAddonProvider.get(appId, namespaceId, stageId, addonName);
        AddonMetaDTO addonMetaDTO = addonMetaProvider.get(appAddonDTO.getAddonId(), appAddonDTO.getAddonVersion());

        Yaml yaml = SchemaUtil.createYaml(ComponentSchema.class);
        ComponentSchema addonSchema = yaml.loadAs(addonMetaDTO.getAddonSchema(), ComponentSchema.class);
        List<WorkloadResource.DataOutput> dataOutputs = addonSchema.getSpec().getWorkload().getDataOutputs();
        return buildSucceedResult(dataOutputs);
    }
}
