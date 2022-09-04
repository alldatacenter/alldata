package com.alibaba.tesla.productops.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.*;
import com.alibaba.tesla.productops.common.JsonUtil;
import com.alibaba.tesla.productops.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/frontend/exImport")
public class ExImportController extends BaseController {

    @Autowired
    ProductopsAppRepository productopsAppRepository;

    @Autowired
    ProductopsNodeRepository productopsNodeRepository;

    @Autowired
    ProductopsElementRepository productopsElementRepository;

    @Autowired
    ProductopsNodeElementRepository productopsNodeElementRepository;

    @Autowired
    ProductopsTabRepository productopsTabRepository;

    @Autowired
    ProductopsComponentRepository productopsComponentRepository;

    Object simple(Object x) {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(x));
        jsonObject.remove("id");
        jsonObject.remove("gmtCreate");
        jsonObject.remove("gmtModified");
        jsonObject.remove("lastModifier");

        // 导出时候将is_import全部设置为1
        if(jsonObject.getInteger("isImport") != null){
            jsonObject.put("isImport", 1);
        }
        return jsonObject;
    }

    Object simple(List<?> list) {
        return list.stream()
            .map(this::simple)
            .collect(Collectors.toList());
    }

    @GetMapping(value = "export")
    public TeslaBaseResult export(String appId, String stageId) {
        JSONObject contentJson = JsonUtil.map(
            "app", simple(productopsAppRepository.findFirstByAppIdAndStageId(appId, stageId)),
            "nodeList", simple(productopsNodeRepository.findAllByNodeTypePathLikeAndStageId(appId + "|%", stageId)),
            "elementList", simple(productopsElementRepository.findAllByAppIdAndStageId(appId, stageId)),
            "nodeElementList", simple(productopsNodeElementRepository.findAllByNodeTypePathLikeAndStageId(appId + "|%", stageId)),
            "tabList", simple(productopsTabRepository.findAllByNodeTypePathLikeAndStageId(appId + "|%", stageId))
        );
        if(appId.equals("system")){
            contentJson.put("componentList", simple(productopsComponentRepository.findAllByStageId("prod")));
        }
        return buildSucceedResult(JSONObject.toJSONString(contentJson, true));
    }

    @PostMapping(value = "import")
    public TeslaBaseResult iimport(@RequestBody String content, String stageId) throws IOException {

        JSONObject jsonObject = JSONObject.parseObject(content);
        ProductopsApp app = jsonObject.getJSONObject("app").toJavaObject(ProductopsApp.class);
        List<ProductopsElement> eList = jsonObject.getJSONArray("elementList")
            .toJavaList(ProductopsElement.class);
        List<ProductopsNodeElement> neList = jsonObject.getJSONArray("nodeElementList")
            .toJavaList(ProductopsNodeElement.class);
        List<ProductopsNode> nodeList = jsonObject.getJSONArray("nodeList")
            .toJavaList(ProductopsNode.class);
        List<ProductopsTab> tabList = jsonObject.getJSONArray("tabList")
            .toJavaList(ProductopsTab.class);
        log.info(
            "start import stageId:{} eSize:{} neSize:{} nSize:{} tSize:{}",
            stageId, eList.size(), neList.size(), nodeList.size(), tabList.size()
        );
        app.setGmtCreate(System.currentTimeMillis());
        ProductopsApp productopsApp = productopsAppRepository.findFirstByAppIdAndStageId(app.getAppId(), stageId);
        if (productopsApp == null) {
            productopsApp = app;
        }
        productopsApp.setGmtModified(System.currentTimeMillis());
        productopsApp.setLastModifier(getUserEmployeeId());
        productopsApp.setStageId(stageId);
        productopsAppRepository.save(productopsApp);

        productopsNodeElementRepository.deleteByAppIdAndStageIdAndIsImport(app.getAppId(), stageId, 1);
        for (ProductopsNodeElement nodeElement : neList) {
            nodeElement.setGmtCreate(System.currentTimeMillis());
            ProductopsNodeElement productopsNodeElement = productopsNodeElementRepository
                .findFirstByNodeTypePathAndElementIdAndStageId(nodeElement.getNodeTypePath(), nodeElement.getElementId(), stageId);
            if (productopsNodeElement == null) {
                productopsNodeElement = nodeElement;
            }
            productopsNodeElement.setGmtModified(System.currentTimeMillis());
            productopsNodeElement.setLastModifier(getUserEmployeeId());
            productopsNodeElement.setStageId(stageId);
            productopsNodeElement.setIsImport(1);
            productopsNodeElementRepository.save(productopsNodeElement);
        }
        productopsNodeElementRepository.flush();

        productopsElementRepository.deleteByAppIdAndStageIdAndIsImport(app.getAppId(), stageId, 1);
        for (ProductopsElement element : eList) {
            element.setGmtCreate(System.currentTimeMillis());
            ProductopsElement productopsElement = productopsElementRepository
                .findFirstByElementIdAndStageId(element.getElementId(), stageId);
            if (productopsElement == null) {
                productopsElement = element;
            }
            productopsElement.setGmtModified(System.currentTimeMillis());
            productopsElement.setLastModifier(getUserEmployeeId());
            productopsElement.setStageId(stageId);
            productopsElement.setIsImport(1);
            productopsElementRepository.save(productopsElement);
        }
        productopsElementRepository.flush();

        productopsNodeRepository.deleteByNodeTypePathLikeAndStageIdAndIsImport(app.getAppId() + "|%", stageId, 1);
        for (ProductopsNode node : nodeList) {
            node.setGmtCreate(System.currentTimeMillis());
            ProductopsNode productopsNode = productopsNodeRepository
                .findFirstByNodeTypePathAndStageId(node.getNodeTypePath(), stageId);
            if (productopsNode == null) {
                productopsNode = node;
            }
            productopsNode.setGmtModified(System.currentTimeMillis());
            productopsNode.setLastModifier(getUserEmployeeId());
            productopsNode.setStageId(stageId);
            productopsNode.setIsImport(1);
            productopsNodeRepository.save(productopsNode);
        }
        productopsNodeRepository.flush();

        productopsTabRepository.deleteByNodeTypePathLikeAndStageIdAndIsImport(app.getAppId() + "|%", stageId, 1);
        for (ProductopsTab tab : tabList) {
            tab.setGmtCreate(System.currentTimeMillis());
            ProductopsTab productopsTab = productopsTabRepository
                .findFirstByTabIdAndStageId(tab.getTabId(), stageId);
            if (productopsTab == null) {
                productopsTab = tab;
            }
            productopsTab.setGmtModified(System.currentTimeMillis());
            productopsTab.setLastModifier(getUserEmployeeId());
            productopsTab.setStageId(stageId);
            productopsTab.setIsImport(1);
            productopsTabRepository.save(productopsTab);
        }
        productopsTabRepository.flush();

        if(jsonObject.getJSONArray("componentList") != null){
            List<ProductopsComponent> productopsComponentList = jsonObject.getJSONArray("componentList").toJavaList(ProductopsComponent.class);

            productopsComponentRepository.deleteByStageIdAndIsImport(stageId, 1);
            for (ProductopsComponent component : productopsComponentList) {
                component.setGmtCreate(System.currentTimeMillis());
                ProductopsComponent productopsComponent = productopsComponentRepository
                        .findFirstByComponentIdAndStageId(component.getComponentId(), stageId);
                if (productopsComponent == null) {
                    productopsComponent = component;
                }
                productopsComponent.setGmtModified(System.currentTimeMillis());
                productopsComponent.setLastModifier(getUserEmployeeId());
                productopsComponent.setStageId(stageId);
                productopsComponentRepository.save(productopsComponent);
            }
            productopsComponentRepository.flush();
        }

        log.info("end import");

        return buildSucceedResult("ok");



    }

}
