package com.alibaba.tesla.productops.controllers;

import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsTab;
import com.alibaba.tesla.productops.common.JsonUtil;
import com.alibaba.tesla.productops.params.TabUpsertParam;
import com.alibaba.tesla.productops.repository.ProductopsTabRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/frontend/nodeTypePaths")
public class TabController extends BaseController {

    @Autowired
    ProductopsTabRepository productopsTabRepository;

    @GetMapping(value = "tabs")
    public TeslaBaseResult getTabs(String nodeTypePath, String stageId) {
        log.debug("{} {}", nodeTypePath, stageId);
        return buildSucceedResult(JsonUtil.map(
            "nodeId", nodeTypePath,
            "tabs", productopsTabRepository.findAllByNodeTypePathAndStageId(nodeTypePath, stageId).stream()
                .map(ProductopsTab::toJSONObject).collect(Collectors.toList()))
        );
    }

    @DeleteMapping(value = "tab")
    public TeslaBaseResult delete(String id, String stageId) {
        productopsTabRepository.deleteByTabIdAndStageId(id, stageId);
        return buildSucceedResult("ok");
    }

    @PutMapping(value = "tab")
    public TeslaBaseResult put(@RequestBody TabUpsertParam param, String stageId) {
        ProductopsTab tab = productopsTabRepository.findFirstByTabIdAndStageId(param.getId(), stageId);
        if (tab == null) {
            tab = ProductopsTab.builder()
                .stageId(stageId)
                .gmtCreate(System.currentTimeMillis())
                .build();
        }

        String appId = param.getNodeTypePath().split("\\|")[0];

        tab.setTabId(param.getId());
        tab.setGmtModified(System.currentTimeMillis());
        tab.setLastModifier(getUserEmployeeId());
        tab.setConfig(JSONObject.toJSONString(param.getConfig()));
        tab.setElements(JSONObject.toJSONString(param.getElements()));
        tab.setNodeTypePath(param.getNodeTypePath());
        tab.setLabel(param.getLabel());
        tab.setAppId(appId);
        tab.setName(param.getName());
        tab.setIsImport(0);
        System.out.println(JSONObject.toJSONString(tab, true));
        if (log.isDebugEnabled()) {
            log.debug(JSONObject.toJSONString(tab, true));
        }
        productopsTabRepository.saveAndFlush(tab);
        return buildSucceedResult(tab);
    }

}
