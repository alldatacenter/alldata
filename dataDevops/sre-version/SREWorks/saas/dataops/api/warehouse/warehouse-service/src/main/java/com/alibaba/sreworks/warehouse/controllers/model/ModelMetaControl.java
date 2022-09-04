package com.alibaba.sreworks.warehouse.controllers.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.model.ModelMetaService;
import com.alibaba.sreworks.warehouse.domain.req.model.*;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型元Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/model/meta/")
@Api(tags = "模型--元数据")
public class ModelMetaControl extends BaseController {

    @Autowired
    ModelMetaService modelMetaService;

    @ApiOperation(value = "统计模型信息(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/statsModelById", method = RequestMethod.GET)
    public TeslaBaseResult statsModelById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(modelMetaService.statsModelById(id));
    }

    @ApiOperation(value = "查询模型信息(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getModelById", method = RequestMethod.GET)
    public TeslaBaseResult getModelMetaById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(modelMetaService.getModelById(id));
    }

    @ApiOperation(value = "查询模型信息(根据模型名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "模型名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getModelByName", method = RequestMethod.GET)
    public TeslaBaseResult getModelByName(@RequestParam(name = "name") String name) {
        return buildSucceedResult(modelMetaService.getModelByName(name));
    }

    @ApiOperation(value = "查询模型信息(根据数据域名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "layer", value = "数仓分层", defaultValue = "dwd", paramType = "query"),
            @ApiImplicitParam(name = "subject", value = "数据主题", defaultValue = "运维对象主题", paramType = "query"),
            @ApiImplicitParam(name = "domainId", value = "数据域ID", defaultValue = "1", paramType = "query")
    })
    @RequestMapping(value = "/getModelsByDomain", method = RequestMethod.GET)
    public TeslaBaseResult getModelsByDomain(@RequestParam(name = "layer", required = false) String layer,
                                             @RequestParam(name = "subject", required = false) String subject,
                                             @RequestParam(name = "domainId", required = false) Integer domainId) {
        if (domainId != null && domainId > 0) {
            return buildSucceedResult(modelMetaService.getModelsByDomain(layer, domainId));
        } else if (StringUtils.isNotEmpty(subject)) {
            return buildSucceedResult(modelMetaService.getModelsBySubject(layer, subject));
        } else if (StringUtils.isNotEmpty(layer)) {
            return buildSucceedResult(modelMetaService.getModelsByLayer(layer));
        } else {
            return buildSucceedResult(modelMetaService.getModels());
        }
    }

    @ApiOperation(value = "查询模型信息(根据数仓分层)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "layer", value = "数仓分层", defaultValue = "dwd", paramType = "query")
    })
    @RequestMapping(value = "/getModelsByLayer", method = RequestMethod.GET)
    public TeslaBaseResult getModelsByLayer(@RequestParam(name = "layer", required = false) String layer) {
        if (StringUtils.isEmpty(layer)) {
            return buildSucceedResult(modelMetaService.getModels());
        } else {
            return buildSucceedResult(modelMetaService.getModelsByLayer(layer));
        }
    }

    @ApiOperation(value = "查询模型信息(所有)")
    @RequestMapping(value = "/getModels", method = RequestMethod.GET)
    public TeslaBaseResult getModels(){
        return buildSucceedResult(modelMetaService.getModels());
    }

    @ApiOperation(value = "查询模型信息(根据ADS模型标签)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tag", value = "模型标签", defaultValue = "服务", paramType = "query")
    })
    @RequestMapping(value = "/getAdsModelsByTag", method = RequestMethod.GET)
    public TeslaBaseResult getAdsModelsByTag(@RequestParam(name = "tag", required = false) String tag) {
        List<JSONObject> adsModels = modelMetaService.getModelsByLayer("ads");
        if (StringUtils.isEmpty(tag)) {
            return buildSucceedResult(adsModels);
        } else {
            return buildSucceedResult(adsModels.stream().filter(adsModel -> adsModel.getString("tag").equals(tag)).collect(Collectors.toList()));
        }
    }

    @ApiOperation(value = "查询模型列信息(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getFieldsByModelId", method = RequestMethod.GET)
    public TeslaBaseResult getFieldsByModelId(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(modelMetaService.getFieldsByModelId(id));
    }

    @ApiOperation(value = "查询模型列信息(根据模型名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "模型名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getFieldsByModelName", method = RequestMethod.GET)
    public TeslaBaseResult getFieldsByModelName(@RequestParam(name = "name") String name) throws Exception {
        return buildSucceedResult(modelMetaService.getFieldsByModelName(name));
    }

    @ApiOperation(value = "查询模型和列信息(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getModelWithFieldsById", method = RequestMethod.GET)
    public TeslaBaseResult getModelWithFieldsById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(modelMetaService.getModelWithFieldsById(id));
    }

    @ApiOperation(value = "查询模型和列信息(根据模型名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "模型名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getModelWithFieldsByName", method = RequestMethod.GET)
    public TeslaBaseResult getModelWithFieldsByName(@RequestParam(name = "name") String name) {
        return buildSucceedResult(modelMetaService.getModelWithFieldsByName(name));
    }

    @ApiOperation(value = "删除模型(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/deleteModelById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteModelById(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(modelMetaService.deleteModelById(id));
    }

    @ApiOperation(value = "删除模型(根据模型名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "模型名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/deleteModelByName", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteModelByName(@RequestParam(name = "name") String name) throws Exception {
        return buildSucceedResult(modelMetaService.deleteModelByName(name));
    }

    @ApiOperation(value = "删除模型列(根据模型ID和列ID,仅删除列定义)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "模型ID", defaultValue = "0", paramType = "query"),
            @ApiImplicitParam(name = "fieldId", value = "列ID", defaultValue = "1", paramType = "query")
    })
    @RequestMapping(value = "/deleteFieldById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteFieldById(@RequestParam(name = "modelId") Long modelId,
                                                 @RequestParam(name = "fieldId") Long fieldId) throws Exception {
        return buildSucceedResult(modelMetaService.deleteFieldById(modelId, fieldId));
    }

    @ApiOperation(value = "删除模型列(根据模型ID和列名,仅删除列定义)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "模型ID", defaultValue = "0", paramType = "query"),
            @ApiImplicitParam(name = "fieldName", value = "列名", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/deleteFieldByName", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteFieldByName(@RequestParam(name = "modelId") Long modelId,
                                                 @RequestParam(name = "fieldName") String fieldName) throws Exception {
        return buildSucceedResult(modelMetaService.deleteFieldByName(modelId, fieldName));
    }

    @ApiOperation(value = "创建模型")
    @RequestMapping(value = "/createModel", method = RequestMethod.POST)
    public TeslaBaseResult createModel(@RequestBody ModelCreateReq req) throws Exception {
        return buildSucceedResult(modelMetaService.createModel(req));
    }

    @ApiOperation(value = "创建模型(带列信息)")
    @RequestMapping(value = "/createModelWithFields", method = RequestMethod.POST)
    public TeslaBaseResult createModelWithFields(@RequestBody ModelWithFieldsCreateReq req) throws Exception {
        return buildSucceedResult(modelMetaService.createModelWithFields(req.getMetaReq(), Arrays.asList(req.getFieldsReq())));
    }

    @ApiOperation(value = "更新模型")
    @RequestMapping(value = "/updateModel", method = RequestMethod.POST)
    public TeslaBaseResult updateModel(@RequestBody ModelUpdateReq req) throws Exception {
        return buildSucceedResult(modelMetaService.updateModel(req));
    }

    @ApiOperation(value = "新增列(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/addFieldByModelId", method = RequestMethod.POST)
    public TeslaBaseResult addFieldByModelId(@RequestParam(name = "modelId") Long modelId,
                                              @RequestBody @ApiParam(value = "模型列") ModelFieldCreateReq fieldReq) throws Exception {
        modelMetaService.addFieldByModelId(modelId, fieldReq);
        return buildSucceedResult("done");
    }

    @ApiOperation(value = "更新列(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "模型ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/updateFieldByModelId", method = RequestMethod.POST)
    public TeslaBaseResult updateFieldByModelId(@RequestParam(name = "modelId") Long modelId,
                                             @RequestBody @ApiParam(value = "模型列") ModelFieldUpdateReq fieldReq) throws Exception {
        modelMetaService.updateFieldByModelId(modelId, fieldReq);
        return buildSucceedResult("done");
    }
}
