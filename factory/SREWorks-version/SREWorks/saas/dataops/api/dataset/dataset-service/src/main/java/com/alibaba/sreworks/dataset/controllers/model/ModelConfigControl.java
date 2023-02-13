package com.alibaba.sreworks.dataset.controllers.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.domain.DomainService;
import com.alibaba.sreworks.dataset.api.model.ModelConfigService;
import com.alibaba.sreworks.dataset.domain.req.model.DataModelConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.model.DataModelConfigUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 数据模型配置信息Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/modelConfig/")
@Api(tags = "数据模型配置信息")
public class ModelConfigControl extends BaseController {

    @Autowired
    DomainService domainService;

    @Autowired
    ModelConfigService modelConfigService;

    @ApiOperation(value = "查询数据模型(根据数据模型ID)")
    @RequestMapping(value = "/getModelConfigById", method = RequestMethod.GET)
    public TeslaBaseResult getModelConfigById(@RequestParam(name = "modeId") Integer modeId) {
        return buildSucceedResult(modelConfigService.getModelConfigById(modeId));
    }

    @ApiOperation(value = "查询数据模型(根据数据域ID)")
    @RequestMapping(value = "/getModelConfigByDomain", method = RequestMethod.GET)
    public TeslaBaseResult getModelConfigByDomain(@RequestParam(name = "domainId", required = false) Integer domainId) {
        return buildSucceedResult(modelConfigService.getModelConfigsByDomain(domainId));
    }

    @ApiOperation(value = "查询数据模型映射(根据数据域ID,前端)")
    @RequestMapping(value = "/getModelIdLabelByDomain", method = RequestMethod.GET)
    public TeslaBaseResult getModelIdLabelByDomain(@RequestParam(name = "domainId", required = false) Integer domainId) {
        List<JSONObject> results = modelConfigService.getModelConfigsByDomain(domainId);
        results.forEach(result -> {
            result.put("label", result.get("label"));
            result.put("value", result.get("id"));
        });
        return buildSucceedResult(results);
    }

    @ApiOperation(value = "查询数据模型(根据数据主题ID)")
    @RequestMapping(value = "/getModelConfigBySubject", method = RequestMethod.GET)
    public TeslaBaseResult getModelConfigBySubject(@RequestParam(name = "subjectId", required = false) Integer subjectId) {
        return buildSucceedResult(modelConfigService.getModelConfigsBySubject(subjectId));
    }

    @ApiOperation(value = "查询数据模型(根据数据主题ID和数据域ID,兼容全量接口)")
    @RequestMapping(value = "/getModelConfig", method = RequestMethod.GET)
    public TeslaBaseResult getModelConfig(@RequestParam(name = "subjectId", required = false) Integer subjectId,
                                          @RequestParam(name = "domainId", required = false) Integer domainId) {
        return buildSucceedResult(modelConfigService.getModelConfigs(subjectId, domainId));
    }

    @ApiOperation(value = "查询数据模型(根据级联数据主题ID和数据域ID)")
    @RequestMapping(value = "/getModelConfigCascade", method = RequestMethod.GET)
    public TeslaBaseResult getModelConfigCascade(@RequestParam(name = "cascadeId", required = false) String cascadeId) {
        Integer subjectId = null;
        Integer domainId = null;
        if (StringUtils.isNotEmpty(cascadeId)) {
            String[] ids = cascadeId.split(",");
            subjectId = Integer.parseInt(ids[0]);
            if (ids.length>1) {
                domainId = Integer.parseInt(ids[1]);
            }
        }
        return buildSucceedResult(modelConfigService.getModelConfigs(subjectId, domainId));
    }

    @ApiOperation(value = "更新数据模型(根据数据模型ID)")
    @RequestMapping(value = "/updateModel", method = RequestMethod.POST)
    public TeslaBaseResult updateModelConfig(@RequestBody DataModelConfigUpdateReq req) throws Exception {
        return buildSucceedResult(modelConfigService.updateModelConfig(req));
    }

    @ApiOperation(value = "新增数据模型")
    @RequestMapping(value = "/createModel", method = RequestMethod.POST)
    public TeslaBaseResult createModel(@RequestBody DataModelConfigCreateReq req) throws Exception {
        return buildSucceedResult(modelConfigService.addModelConfig(req));
    }

    @ApiOperation(value = "删除数据模型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "模型ID", paramType = "query")
    })
    @RequestMapping(value = "/deleteModelById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteModelById(@RequestParam(name = "modelId") Integer modelId) throws Exception {
        return buildSucceedResult(modelConfigService.deleteModelById(modelId));
    }

    @ApiOperation(value = "查询数据模型条件列表(根据数据模型ID)")
    @RequestMapping(value = "/getModelQueryFieldsById", method = RequestMethod.GET)
    public TeslaBaseResult getModelQueryFieldsById(@RequestParam(name = "modeId") Integer modeId) {
        return buildSucceedResult(modelConfigService.getModelQueryFieldsById(modeId));
    }

    @ApiOperation(value = "查询数据模型字段列表(根据数据模型ID)")
    @RequestMapping(value = "/getModelValueFieldsById", method = RequestMethod.GET)
    public TeslaBaseResult getModelValueFieldsById(@RequestParam(name = "modeId") Integer modeId) {
        return buildSucceedResult(modelConfigService.getModelValueFieldsById(modeId));
    }

    @ApiOperation(value = "查询数据模型分组列表(根据数据模型ID)")
    @RequestMapping(value = "/getModelGroupFieldsById", method = RequestMethod.GET)
    public TeslaBaseResult getModelGroupFieldsById(@RequestParam(name = "modeId") Integer modeId) {
        return buildSucceedResult(modelConfigService.getModelGroupFieldsById(modeId));
    }
}
