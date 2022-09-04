package com.alibaba.sreworks.dataset.controllers.inter;

import com.alibaba.sreworks.dataset.api.inter.DataInterfaceConfigService;
import com.alibaba.sreworks.dataset.api.model.ModelConfigService;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceCreateComplexReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceUpdateComplexReq;
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

import java.util.Arrays;


/**
 * 数据接口配置信息Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/interfaceConfig/")
@Api(tags = "数据接口配置信息")
public class DataInterfaceConfigControl extends BaseController {

    @Autowired
    ModelConfigService modelConfigService;

    @Autowired
    DataInterfaceConfigService interfaceConfigService;

    @ApiOperation(value = "查询数据接口配置(根据数据接口ID)")
    @RequestMapping(value = "/getInterfaceConfigById", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceConfigById(@RequestParam(name = "interfaceId") Integer interfaceId) {
        return buildSucceedResult(interfaceConfigService.getConfigById(interfaceId));
    }

    @ApiOperation(value = "查询数据接口配置(根据数据模型ID)")
    @RequestMapping(value = "/getInterfaceConfigByModel", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceConfigByModel(@RequestParam(name = "modelId", required = false) Integer modelId) {
        if (modelId == null) {
            modelId = 0;
        }
        return buildSucceedResult(interfaceConfigService.getConfigsByModel(modelId));
    }

    @ApiOperation(value = "查询数据接口配置(根据数据域ID)")
    @RequestMapping(value = "/getInterfaceConfigByDomain", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceConfigByDomain(@RequestParam(name = "domainId", required = false) Integer domainId) {
        return buildSucceedResult(interfaceConfigService.getConfigsByDomain(domainId));
    }

    @ApiOperation(value = "查询数据接口配置")
    @RequestMapping(value = "/getInterfaceConfig", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceConfig(@RequestParam(name = "subjectId", required = false) Integer subjectId,
                                              @RequestParam(name = "domainId", required = false) Integer domainId,
                                              @RequestParam(name = "modelId", required = false) Integer modelId) {
        return buildSucceedResult(interfaceConfigService.getConfigs(subjectId, domainId, modelId));
    }

    @ApiOperation(value = "查询数据接口配置(根据级联数据主题ID和数据域ID)")
    @RequestMapping(value = "/getInterfaceConfigCascade", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceConfigCascade(@RequestParam(name = "cascadeId", required = false) String cascadeId) {
        Integer subjectId = null;
        Integer domainId = null;
        Integer modelId = null;
        if (StringUtils.isNotEmpty(cascadeId)) {
            String[] ids = cascadeId.split(",");
            subjectId = Integer.parseInt(ids[0]);
            if (ids.length>1) {
                domainId = Integer.parseInt(ids[1]);
            }
            if (ids.length>2) {
                modelId = Integer.parseInt(ids[2]);
            }
        }

        return buildSucceedResult(interfaceConfigService.getConfigs(subjectId, domainId, modelId));
    }

    @ApiOperation(value = "新增数据接口")
    @RequestMapping(value = "/createInterfaceWithParams", method = RequestMethod.POST)
    public TeslaBaseResult createInterfaceWithParams(@RequestBody DataInterfaceCreateComplexReq req) throws Exception {
        return buildSucceedResult(interfaceConfigService.addInterfaceConfigWithParams(req, Arrays.asList(req.getParams())));
    }

    @ApiOperation(value = "更新数据接口")
    @RequestMapping(value = "/updateInterfaceWithParams", method = RequestMethod.POST)
    public TeslaBaseResult updateModelConfig(@RequestBody DataInterfaceUpdateComplexReq req) throws Exception {
        return buildSucceedResult(interfaceConfigService.updateInterfaceConfigWithParams(req, Arrays.asList(req.getParams())));
    }

    @ApiOperation(value = "删除数据接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "interfaceId", value = "接口ID", paramType = "query")
    })
    @RequestMapping(value = "/deleteInterface", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteInterface(@RequestParam(name = "interfaceId") Integer interfaceId) throws Exception {
        return buildSucceedResult(interfaceConfigService.deleteInterfaceById(interfaceId));
    }

}
