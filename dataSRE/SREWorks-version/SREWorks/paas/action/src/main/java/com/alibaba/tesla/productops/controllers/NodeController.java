package com.alibaba.tesla.productops.controllers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsElement;
import com.alibaba.tesla.productops.DO.ProductopsNode;
import com.alibaba.tesla.productops.DO.ProductopsNodeElement;
import com.alibaba.tesla.productops.DO.ProductopsTab;
import com.alibaba.tesla.productops.common.StringUtil;
import com.alibaba.tesla.productops.params.NodeCloneParam;
import com.alibaba.tesla.productops.params.NodeDeleteParam;
import com.alibaba.tesla.productops.params.NodeInsertParam;
import com.alibaba.tesla.productops.params.NodeUpdateParam;
import com.alibaba.tesla.productops.repository.ProductopsElementRepository;
import com.alibaba.tesla.productops.repository.ProductopsNodeElementRepository;
import com.alibaba.tesla.productops.repository.ProductopsNodeRepository;
import com.alibaba.tesla.productops.repository.ProductopsTabRepository;
import com.alibaba.tesla.productops.services.NodeAddUrlService;
import com.alibaba.tesla.productops.services.NodeService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/frontend/appTrees")
public class NodeController extends BaseController {

    @Autowired
    ProductopsNodeRepository productopsNodeRepository;

    @Autowired
    ProductopsTabRepository productopsTabRepository;

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeAddUrlService nodeAddUrlService;

    @Autowired
    ProductopsNodeElementRepository productopsNodeElementRepository;

    @Autowired
    ProductopsElementRepository productopsElementRepository;

    private String getNodeTypePath(NodeInsertParam param) {
        String parentNodeTypePath = param.getParentNodeTypePath();
        String serviceType = param.getServiceType();
        if (StringUtil.isEmpty(parentNodeTypePath)) {
            return serviceType;
        }
        return parentNodeTypePath.endsWith(":") ?
            parentNodeTypePath + serviceType : parentNodeTypePath + "::" + serviceType;
    }

    @GetMapping(value = "/structures")
    public TeslaBaseResult get(String treeType, String appId, String stageId) {
        String nodeTypePath = appId + "|app|T:";
        JSONObject tree = nodeService.tree(nodeTypePath, appId, stageId);
        return buildSucceedResult(tree);
    }

    @Transactional(rollbackOn = Exception.class)
    @PostMapping(value = "/clone")
    public TeslaBaseResult clone(@RequestBody NodeCloneParam param, String stageId) {
//        ProductopsNode sourceNode = nodeService.getNode(param.getSourceNodeTypePath(), stageId);
        ProductopsNode node = nodeService.getNode(param.getTargetNodeTypePath(), stageId);

        ProductopsTab sourceTab = productopsTabRepository.findFirstByNodeTypePathAndStageId(param.getSourceNodeTypePath(), stageId);

        String targetApp = param.getTargetNodeTypePath().split("\\|")[0];

        // clone tab
        String tabId = UUID.randomUUID().toString();
        ProductopsTab tab = ProductopsTab.builder()
                .stageId(stageId)
                .gmtCreate(System.currentTimeMillis())
                .build();

        tab.setTabId(tabId);
        tab.setGmtModified(System.currentTimeMillis());
        tab.setLastModifier(getUserEmployeeId());
        tab.setConfig(sourceTab.getConfig());
        tab.setElements(sourceTab.getElements());
        tab.setNodeTypePath(param.getTargetNodeTypePath());
        tab.setLabel(tabId);
        tab.setName(tabId);
        tab.setIsImport(0);

        // clone elements
        List<ProductopsNodeElement> sourceNodeElements = productopsNodeElementRepository.findAllByNodeTypePathLikeAndStageId(
           param.getSourceNodeTypePath(), stageId);

        Map<String, ProductopsNodeElement> sourceNodeElementMap = sourceNodeElements.stream().collect(Collectors.toMap(
           ProductopsNodeElement::getElementId, x-> x
        ));

        List<String> elementIdList = sourceNodeElements.stream().map(x -> x.getElementId()).collect(Collectors.toList());
        List<ProductopsElement> elements = productopsElementRepository.findAllByElementIdInAndStageId(elementIdList, stageId);

        List<ProductopsElement> newElements = new ArrayList<>();
        List<ProductopsNodeElement> newNodeElements = new ArrayList<>();
        Map<String, String> elementsIdMaps = new HashMap<>();

        for(ProductopsElement element : elements){

            ProductopsNodeElement sourceNodeElement = sourceNodeElementMap.get(element.getElementId());

            String newUuid = UUID.randomUUID().toString();
            String newElementId = targetApp + ":" + sourceNodeElement.getElementId().split(":")[1] + ":" + newUuid;
            elementsIdMaps.put(element.getElementId(), newElementId);

            ProductopsNodeElement newNodeElement = ProductopsNodeElement.builder()
                    .stageId(stageId)
                    .gmtCreate(System.currentTimeMillis())
                    .build();

            newNodeElement.setGmtModified(System.currentTimeMillis());
            newNodeElement.setLastModifier(getUserEmployeeId());
            newNodeElement.setNodeTypePath(param.getTargetNodeTypePath());
            newNodeElement.setElementId(newElementId);
            newNodeElement.setNodeName(sourceNodeElement.getNodeName());
            newNodeElement.setType(sourceNodeElement.getType());
            newNodeElement.setAppId(targetApp);
            newNodeElement.setTags(sourceNodeElement.getTags());
            newNodeElement.setNodeOrder(sourceNodeElement.getNodeOrder());
            newNodeElement.setConfig(sourceNodeElement.getConfig());
            newNodeElement.setIsImport(0);

            newNodeElements.add(newNodeElement);

            ProductopsElement newElement = ProductopsElement.builder()
                    .elementId(newElementId)
                    .stageId(stageId)
                    .gmtCreate(System.currentTimeMillis())
                    .build();

            newElement.setGmtModified(System.currentTimeMillis());
            newElement.setLastModifier(getUserEmployeeId());
            newElement.setName(newUuid);
            newElement.setVersion(element.getVersion());
            newElement.setAppId(targetApp);
            newElement.setType(element.getType());
            newElement.setConfig(element.getConfig());
            newElement.setIsImport(0);

            newElements.add(newElement);
        }

        log.info("elementsIdMaps: {}", elementsIdMaps);

        String newTabElements = tab.getElements();
        // 开始进行element引用关系替换
        for(String oldElementId: elementsIdMaps.keySet()){
            String newElementId = elementsIdMaps.get(oldElementId);
            newTabElements = newTabElements.replaceAll(oldElementId, newElementId);
        }
        tab.setElements(newTabElements);

        for(ProductopsElement newElement : newElements){

            JSONObject newElementConfig = JSONObject.parseObject(newElement.getConfig());

            newElementConfig.put("elementId", newElement.getElementId());
            newElementConfig.put("appId", newElement.getAppId());
            newElementConfig.put("name", newElement.getName());
            newElementConfig.put("id", newElement.getName());

            String newElementConfigRaw = newElementConfig.toJSONString();
            for(String oldElementId: elementsIdMaps.keySet()){
                String newElementId = elementsIdMaps.get(oldElementId);
                newElementConfigRaw = newElementConfigRaw.replaceAll(oldElementId, newElementId);
            }
            newElement.setConfig(newElementConfigRaw);

        }

        productopsTabRepository.saveAndFlush(tab);

        productopsNodeElementRepository.saveAll(newNodeElements);
        productopsNodeElementRepository.flush();

        productopsElementRepository.saveAll(newElements);
        productopsElementRepository.flush();


        return buildSucceedResult(node);


    }

    @PostMapping(value = "/structure")
    public TeslaBaseResult post(@RequestBody NodeInsertParam param, String stageId) {

        log.debug("POST: " + JSONObject.toJSONString(param));
        String nodeTypePath = getNodeTypePath(param);

        String appId = nodeTypePath.split("\\|")[0];

        ProductopsNode node = ProductopsNode.builder()
            .gmtCreate(System.currentTimeMillis())
            .gmtModified(System.currentTimeMillis())
            .lastModifier(getUserEmployeeId())
            .stageId(stageId)
            .appId(appId)
            .category(param.getCategory())
            .parentNodeTypePath(param.getParentNodeTypePath())
            .serviceType(param.getServiceType())
            .nodeTypePath(nodeTypePath)
            .version(param.getVersion())
            .isImport(0)
            .config(JSONObject.toJSONString(param.getConfig()))
            .build();

        productopsNodeRepository.saveAndFlush(node);
        nodeAddUrlService.addUrl(node);
        return buildSucceedResult(node);

    }

    @PutMapping(value = "/structure")
    public TeslaBaseResult put(@RequestBody NodeUpdateParam param, String stageId) {
        if (log.isDebugEnabled()) {
            log.debug("PUT: " + JSONObject.toJSONString(param));
        }
        String[] words = param.getNodeTypePath().split(":");
        String serviceType = words[words.length - 1];

        ProductopsNode node = productopsNodeRepository.findFirstByNodeTypePathAndStageId(
            param.getNodeTypePath(), stageId);

        if (node == null) {
            node = ProductopsNode.builder()
                .gmtCreate(System.currentTimeMillis())
                .stageId(stageId)
                .serviceType(serviceType)
                .nodeTypePath(param.getNodeTypePath())
                .build();
        }
        List<String> roleList = param.subtractionRoleList(node);
        String parentNodeTypePath = param.getNodeTypePath().replace(":" + serviceType, "");
        parentNodeTypePath = StringUtils.strip(parentNodeTypePath, ":");
        if (!param.getNodeTypePath().endsWith(":") && !parentNodeTypePath.contains(":")) {
            parentNodeTypePath = parentNodeTypePath + ":";
        }
        node.setStageId(stageId);
        node.setParentNodeTypePath(parentNodeTypePath);
        node.setGmtModified(System.currentTimeMillis());
        node.setLastModifier(getUserEmployeeId());
        node.setVersion(param.getVersion());
        node.setConfig(JSONObject.toJSONString(param.getConfig()));
        productopsNodeRepository.saveAndFlush(node);
        nodeAddUrlService.addUrl(node);
        if (log.isDebugEnabled()) {
            log.debug(JSONObject.toJSONString(roleList));
        }
        for (String role : roleList) {
            nodeService.cleanNodesRole(node, role, stageId);
        }
        return buildSucceedResult("OK");

    }

    @DeleteMapping(value = "/structure")
    public TeslaBaseResult delete(@RequestBody NodeDeleteParam param, String stageId) {
        return buildSucceedResult(productopsNodeRepository
            .deleteByNodeTypePathAndStageId(param.getNodeTypePath(), stageId));
    }

    @DeleteMapping(value = "/cleanNodesRole")
    public TeslaBaseResult cleanNodesRole(String nodeTypePath, String role, String stageId) {
        ProductopsNode node = productopsNodeRepository.findFirstByNodeTypePathAndStageId(nodeTypePath, stageId);
        nodeService.cleanNodesRole(node, role, stageId);
        return buildSucceedResult("OK");
    }

}
