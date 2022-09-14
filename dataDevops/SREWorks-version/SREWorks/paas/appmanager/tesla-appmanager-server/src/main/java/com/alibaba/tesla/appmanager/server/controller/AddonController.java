package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.AddonMetaProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.dto.AddonMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.AddonMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.appaddon.AppAddonSyncReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Addon 元信息 Controller
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@RequestMapping("/addon")
@RestController
@Slf4j
public class AddonController extends AppManagerBaseController {

    @Autowired
    private AddonMetaProvider addonMetaProvider;

    /**
     * @api {get} /addon 获取 Addon 列表
     * @apiName GetAddonList
     * @apiGroup Addon API
     * @apiParam (GET Parameters) {String} addonId 过滤条件：Addon ID
     * @apiParam (GET Parameters) {String} addonVersion 过滤条件：Addon 版本
     * @apiParam (GET Parameters) {String[]} addonTypeList 过滤条件：Addon 类型列表 (可选 MICROSERVICE, K8S_MICROSERVICE, K8S_JOB, RESOURCE_ADDON, INTERNAL_ADDON, TRAIT_ADDON, CUSTOM_ADDON, ABM_CHART)
     * @apiParam (GET Parameters) {Number} page 当前页
     * @apiParam (GET Parameters) {Number} pageSize 每页大小
     */
    @GetMapping
    public TeslaBaseResult list(@ModelAttribute AddonMetaQueryReq request) {
        return buildSucceedResult(addonMetaProvider.list(request));
    }

    // 新增 Addon
    @PostMapping
    public TeslaBaseResult save(@RequestBody AddonMetaDTO metaDTO) {
        return buildSucceedResult(addonMetaProvider.create(metaDTO));
    }

    // 获取指定 Addon
    @GetMapping("/{id}")
    public TeslaBaseResult getById(@PathVariable("id") Long id) {
        return buildSucceedResult(addonMetaProvider.get(id));
    }

    // 同步全量 addon 绑定关系
    @PutMapping("/sync")
    public TeslaBaseResult sync(@RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) {
        BizAppContainer container = BizAppContainer.valueOf(headerBizApp);
        String namespaceId = container.getNamespaceId();
        String stageId = container.getStageId();
        AppAddonSyncReq req = AppAddonSyncReq.builder()
                .namespaceId(namespaceId)
                .stageId(stageId)
                .build();
        addonMetaProvider.sync(req);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }

    // 更新指定 Addon
    @PutMapping("/{id}")
    public TeslaBaseResult update(@PathVariable("id") Long id, @RequestBody AddonMetaDTO metaDTO) {
        metaDTO.setId(id);
        addonMetaProvider.update(metaDTO);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }

    // 删除指定 Addon
    @DeleteMapping("/{id}")
    public TeslaBaseResult update(@PathVariable("id") Long id) {
        addonMetaProvider.delete(id);
        return buildSucceedResult(DefaultConstant.EMPTY_OBJ);
    }
}
