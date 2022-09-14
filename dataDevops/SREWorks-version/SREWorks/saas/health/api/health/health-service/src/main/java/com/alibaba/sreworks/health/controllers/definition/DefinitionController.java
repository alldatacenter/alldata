package com.alibaba.sreworks.health.controllers.definition;

import com.alibaba.sreworks.health.api.definition.DefinitionService;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionCreateReq;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 事件定义入口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 15:14
 */
@RestController
@RequestMapping("/definition/")
@Api(tags="定义")
public class DefinitionController extends BaseController {

    @Autowired
    DefinitionService definitionService;

    @ApiOperation(value = "查询Header")
    @GetMapping(value = "header")
    public String readAll(@RequestHeader Map<String, String> headers) {
        StringBuilder content = new StringBuilder();
        headers.forEach((key, value) -> content.append(key).append("=").append(value).append(";"));
        return "Read HTTP Headers: " + content.toString();
    }

    @ApiOperation(value = "定义统计数据")
    @RequestMapping(value = "getDefinitionsStat", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitionsStat() {
        return buildSucceedResult(definitionService.getDefinitionsStat());
    }

    @ApiOperation(value = "定义实例统计数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getInstancesStat", method = RequestMethod.GET)
    public TeslaBaseResult getInstancesStat(@RequestParam(name = "appInstanceId", required = false) String appInstanceId) {
        return buildSucceedResult(definitionService.getInstancesStat(appInstanceId));
    }

    @ApiOperation(value = "当日实例新增数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getCurrentDayInstancesInc", method = RequestMethod.GET)
    public TeslaBaseResult getCurrentDayInstancesInc(@RequestParam(name = "appInstanceId", required = false) String appInstanceId) {
        return buildSucceedResult(definitionService.getCurrentDayInstancesInc(appInstanceId));
    }

    @ApiOperation(value = "查询定义(根据id)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query")
    })
    @RequestMapping(value = "getDefinitionsById", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitionsById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(definitionService.getDefinitionById(id));
    }

    @ApiOperation(value = "查询定义列表(根据分类)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", value = "定义分类", paramType = "query")
    })
    @RequestMapping(value = "getDefinitionsByCategory", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitionsByCategory(@RequestParam(name = "category") String category) {
        return buildSucceedResult(definitionService.getDefinitionsByCategory(category.toLowerCase()));
    }


    @ApiOperation(value = "查询定义列表(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getDefinitionsByApp", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitionsByApp(@RequestParam(name = "appId") String appId) {
        return buildSucceedResult(definitionService.getDefinitionsByApp(appId));
    }

    @ApiOperation(value = "查询定义列表(根据应用组件)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件名称", paramType = "query")
    })
    @RequestMapping(value = "getDefinitionsByAppComponent", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitionsByAppComponent(@RequestParam(name = "appId") String appId,
                                                        @RequestParam(name = "appComponentName") String appComponentName) {
        return buildSucceedResult(definitionService.getDefinitionsByAppComponent(appId, appComponentName));
    }

    @ApiOperation(value = "查询定义列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件名称", paramType = "query"),
            @ApiImplicitParam(name = "name", value = "定义名称", paramType = "query"),
            @ApiImplicitParam(name = "category", value = "定义分类", paramType = "query")
    })
    @RequestMapping(value = "getDefinitions", method = RequestMethod.GET)
    public TeslaBaseResult getDefinitions(@RequestParam(name = "appId", required = false) String appId,
                                          @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                          @RequestParam(name = "name", required = false) String name,
                                          @RequestParam(name = "category", required = false) String category) {
        category = StringUtils.isNotEmpty(category) ? category.toLowerCase() : category;
        return buildSucceedResult(definitionService.getDefinitions(appId, appComponentName, name, category));
    }

    @ApiOperation(value = "创建定义")
    @RequestMapping(value = "createDefinition", method = RequestMethod.POST)
    public TeslaBaseResult createDefinition(@RequestHeader(name = "x-empid", required = false) String userId,
                                            @RequestBody DefinitionCreateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(definitionService.addDefinition(req));
    }

    @ApiOperation(value = "更新定义")
    @RequestMapping(value = "updateDefinition", method = RequestMethod.POST)
    public TeslaBaseResult updateDefinition(@RequestHeader(name = "x-empid", required = false) String userId,
                                            @RequestBody DefinitionUpdateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(definitionService.updateDefinition(req));
    }

    @ApiOperation(value = "删除定义")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteDefinition", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteDefinition(@RequestParam(name = "id") Integer id) throws Exception {
        return buildSucceedResult(definitionService.deleteDefinition(id));
    }
}
