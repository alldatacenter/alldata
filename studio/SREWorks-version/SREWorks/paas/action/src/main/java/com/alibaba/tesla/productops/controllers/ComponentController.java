package com.alibaba.tesla.productops.controllers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.common.TeslaBaseResult;
import com.alibaba.tesla.action.controller.BaseController;
import com.alibaba.tesla.productops.DO.ProductopsComponent;
import com.alibaba.tesla.productops.common.StringUtil;
import com.alibaba.tesla.productops.params.ComponentDeleteParam;
import com.alibaba.tesla.productops.params.ComponentInsertParam;
import com.alibaba.tesla.productops.params.ComponentUpdateParam;
import com.alibaba.tesla.productops.repository.ProductopsComponentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * @author jiongen.zje
 */
@Slf4j
@RestController
@RequestMapping("/frontend/component")
public class ComponentController extends BaseController {

    @Autowired
    ProductopsComponentRepository productopsComponentRepository;

    @GetMapping(value = "/list")
    public TeslaBaseResult get(String stageId) {

        return buildSucceedResult(
                productopsComponentRepository.findAllByStageId(stageId).stream().
                map(ProductopsComponent::toJSONObject).collect(Collectors.toList())
        );
    }

    @PostMapping(value = "/create")
    public TeslaBaseResult post(@RequestBody ComponentInsertParam param, String stageId) {

        log.debug("POST: " + JSONObject.toJSONString(param));

        if(!StringUtil.isEmpty(param.getStageId())){
            stageId = param.getStageId();
        }

        ProductopsComponent component = ProductopsComponent.builder()
                .gmtCreate(System.currentTimeMillis())
                .gmtModified(System.currentTimeMillis())
                .lastModifier(getUserEmployeeId())
                .stageId(stageId)
                .name(param.getName())
                .alias(param.getAlias())
                .config(JSONObject.toJSONString(param.getConfig()))
                .componentId(param.getComponentId())
                .isImport(0)
                .interfaces(JSONObject.toJSONString(param.getInterfaces()))
                .build();

        this.productopsComponentRepository.saveAndFlush(component);
        return buildSucceedResult(component);

    }

    @PutMapping(value = "/update")
    public TeslaBaseResult put(@RequestBody ComponentUpdateParam param, String stageId) {

        log.debug("PUT: " + JSONObject.toJSONString(param));

        if(!StringUtil.isEmpty(param.getStageId())){
            stageId = param.getStageId();
        }

        ProductopsComponent componentInfo = productopsComponentRepository.findFirstByComponentIdAndStageId(param.getComponentId(), stageId);
        if(componentInfo != null){
            componentInfo.setGmtModified(System.currentTimeMillis());
            componentInfo.setLastModifier(getUserEmployeeId());
            componentInfo.setStageId(stageId);
            componentInfo.setName(param.getName());
            componentInfo.setAlias(param.getAlias());
            componentInfo.setConfig(JSONObject.toJSONString(param.getConfig()));
            componentInfo.setComponentId(param.getComponentId());
            componentInfo.setInterfaces(JSONObject.toJSONString(param.getInterfaces()));
            this.productopsComponentRepository.saveAndFlush(componentInfo);
            return buildSucceedResult(componentInfo);
        }else{
            return buildSucceedResult("NO COMPONENT");
        }
    }

    @DeleteMapping(value = "/delete")
    public TeslaBaseResult delete(@RequestBody ComponentDeleteParam param, String stageId) {

        if(!StringUtil.isEmpty(param.getStageId())){
            stageId = param.getStageId();
        }
        log.debug("DELETE COMPONENT: {} {}", param.getComponentId(), stageId);

        return buildSucceedResult(productopsComponentRepository.deleteByComponentIdAndStageId(param.getComponentId(), stageId));
    }



}
