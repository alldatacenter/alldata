package com.alibaba.sreworks.pmdb.controllers.datasource;

import com.alibaba.sreworks.pmdb.api.datasource.DataSourceService;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 数据源Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 15:50
 */
@Slf4j
@RestController
@RequestMapping("/datasource/")
@Api(tags = "数据源")
public class DataSourceController extends BaseController {

    @Autowired
    DataSourceService dataSourceService;

    @ApiOperation(value = "查询数据源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据源ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getDatasourceById", method = RequestMethod.GET)
    public TeslaBaseResult getDataSourceById(@RequestParam(name = "id", defaultValue = "0") Integer id) {
        return buildSucceedResult(dataSourceService.getDataSourceById(id));
    }

    @ApiOperation(value = "查询数据源(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getDatasourceByApp", method = RequestMethod.GET)
    public TeslaBaseResult getDataSourceByApp(@RequestParam(name = "appId", defaultValue = "0") String appId) {
        return buildSucceedResult(dataSourceService.getDataSourceWithCommonByApp(appId));
    }

    @ApiOperation(value = "查询数据源(根据类型)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", defaultValue = "0", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "数据源类型", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getDatasourceByType", method = RequestMethod.GET)
    public TeslaBaseResult getDataSourceByType(@RequestParam(name = "appId", defaultValue = "0") String appId,
                                              @RequestParam(name = "type", defaultValue = "0") String type) {
        return buildSucceedResult(dataSourceService.getDataSourceWithCommonByType(appId, type));
    }

    @ApiOperation(value = "新增数据源")
    @RequestMapping(value = "/createDatasource", method = RequestMethod.POST)
    public TeslaBaseResult createDataSource(@RequestHeader(name = "x-empid", required = false) String userId,
                                            @RequestBody DataSourceCreateReq req) {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(dataSourceService.createDataSource(req));
    }

    @ApiOperation(value = "更新数据源")
    @RequestMapping(value = "/updateDatasource", method = RequestMethod.POST)
    public TeslaBaseResult updateDataSource(@RequestHeader(name = "x-empid", required = false) String userId,
                                            @RequestBody DataSourceUpdateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(dataSourceService.updateDataSource(req));
    }

    @ApiOperation(value = "删除数据源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "数据源ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/deleteDataSourceById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteDataSourceById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(dataSourceService.deleteDataSourceById(id));
    }
}
