package com.alibaba.tesla.productops.controllers;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsElement;
import com.alibaba.tesla.productops.DO.ProductopsNodeElement;
import com.alibaba.tesla.productops.params.NodeElementDeleteParam;
import com.alibaba.tesla.productops.params.NodeElementUpsertParam;
import com.alibaba.tesla.productops.repository.ProductopsElementRepository;
import com.alibaba.tesla.productops.repository.ProductopsNodeElementRepository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class NodeElementController extends BaseController {

    @Autowired
    ProductopsNodeElementRepository productopsNodeElementRepository;

    @Autowired
    ProductopsElementRepository productopsElementRepository;

    @GetMapping(value = "elements")
    public TeslaBaseResult getElements(String nodeTypePath, String stageId) {
        List<ProductopsNodeElement> nodeElements = productopsNodeElementRepository
            .findAllByNodeTypePathAndStageId(nodeTypePath, stageId);

        return buildSucceedResult(
            productopsElementRepository.findAllByElementIdInAndStageId(
                nodeElements.stream().map(ProductopsNodeElement::getElementId).collect(Collectors.toList()), stageId
            )
                .stream().map(ProductopsElement::toJSONObject).collect(Collectors.toList())
        );
    }

    @GetMapping(value = "element")
    public TeslaBaseResult get(String nodeTypePath, String elementId, String stageId) {
        return buildSucceedResult(
            productopsNodeElementRepository
                .findFirstByNodeTypePathAndElementIdAndStageId(nodeTypePath, elementId, stageId)
        );
    }

    @DeleteMapping(value = "element")
    public TeslaBaseResult delete(@RequestBody NodeElementDeleteParam param, String stageId) {
        productopsNodeElementRepository.deleteByNodeTypePathAndElementIdAndStageId(
            param.getNodeTypePath(),
            param.getElementId(), stageId
        );
        return buildSucceedResult("ok");
    }

    @DeleteMapping(value = "elements")
    public TeslaBaseResult deletes(@RequestBody NodeElementDeleteParam param, String stageId) {
        productopsNodeElementRepository.deleteByNodeTypePathAndStageId(param.getNodeTypePath(), stageId);
        return buildSucceedResult("ok");
    }

    @PutMapping(value = "element")
    public TeslaBaseResult put(@RequestBody NodeElementUpsertParam param, String stageId) {
        ProductopsNodeElement nodeElement = productopsNodeElementRepository
            .findFirstByNodeTypePathAndElementIdAndStageId(
                param.getNodeTypePath(), param.getElementId(), stageId
            );
        if (nodeElement == null) {
            nodeElement = ProductopsNodeElement.builder()
                .stageId(stageId)
                .gmtCreate(System.currentTimeMillis())
                .build();
        }
        String appId = param.getNodeTypePath().split("\\|")[0];

        nodeElement.setGmtModified(System.currentTimeMillis());
        nodeElement.setLastModifier(getUserEmployeeId());
        nodeElement.setNodeTypePath(param.getNodeTypePath());
        nodeElement.setElementId(param.getElementId());
        nodeElement.setNodeName(param.getName());
        nodeElement.setType(param.getType());
        nodeElement.setAppId(appId);
        nodeElement.setTags(param.getTags());
        nodeElement.setNodeOrder(param.getOrder());
        nodeElement.setConfig(JSONObject.toJSONString(param.getConfig()));
        nodeElement.setIsImport(0);
        productopsNodeElementRepository.saveAndFlush(nodeElement);
        return buildSucceedResult(nodeElement);
    }

}
