package com.alibaba.tesla.productops.controllers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsElement;
import com.alibaba.tesla.productops.params.ElementUpsertParam;
import com.alibaba.tesla.productops.repository.ProductopsElementRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/frontend")
public class ElementController extends BaseController {

    @Autowired
    ProductopsElementRepository productopsElementRepository;

    @PutMapping(value = "element")
    public TeslaBaseResult put(@RequestBody ElementUpsertParam param, String stageId) {
        ProductopsElement element = null;
        if (param.getElementId() != null) {
            element = productopsElementRepository.findFirstByElementIdAndStageId(param.getElementId(), stageId);
        }
        if (element == null) {
            element = ProductopsElement.builder()
                .elementId(param.elementId())
                .stageId(stageId)
                .gmtCreate(System.currentTimeMillis())
                .build();
        }
        element.setGmtModified(System.currentTimeMillis());
        element.setLastModifier(getUserEmployeeId());
        element.setName(param.getName());
        element.setVersion(param.getVersion());
        element.setAppId(param.getAppId());
        element.setType(param.getType());
        element.setConfig(JSONObject.toJSONString(param.getConfig()));
        element.setIsImport(0);
        productopsElementRepository.saveAndFlush(element);
        return buildSucceedResult(element);
    }

}
