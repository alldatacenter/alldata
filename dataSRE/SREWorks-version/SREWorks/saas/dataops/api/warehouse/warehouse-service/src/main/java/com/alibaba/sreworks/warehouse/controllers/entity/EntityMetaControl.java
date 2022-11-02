package com.alibaba.sreworks.warehouse.controllers.entity;

import com.alibaba.sreworks.warehouse.api.entity.EntityMetaService;
import com.alibaba.sreworks.warehouse.domain.req.entity.*;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 实体元Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/entity/meta/")
@Api(tags = "实体--元数据")
public class EntityMetaControl extends BaseController {

    @Autowired
    EntityMetaService entityMetaService;

    @ApiOperation(value = "统计实体信息(根据模型ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/statsEntityById", method = RequestMethod.GET)
    public TeslaBaseResult statsEntityById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(entityMetaService.statsEntityById(id));
    }

    @ApiOperation(value = "查询实体信息(根据实体ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getEntityById", method = RequestMethod.GET)
    public TeslaBaseResult getEntityMetaById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(entityMetaService.getEntityById(id));
    }

    @ApiOperation(value = "查询实体信息(根据实体名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "实体名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getEntityByName", method = RequestMethod.GET)
    public TeslaBaseResult getEntityByName(@RequestParam(name = "name") String name) {
        return buildSucceedResult(entityMetaService.getEntityByName(name));
    }

    @ApiOperation(value = "查询实体信息(根据数仓分层)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "layer", value = "数仓分层", defaultValue = "ods", paramType = "query")
    })
    @RequestMapping(value = "/getEntitiesByLayer", method = RequestMethod.GET)
    public TeslaBaseResult getEntitiesByLayer(@RequestParam(name = "layer") String layer) {
        return buildSucceedResult(entityMetaService.getEntitiesByLayer(layer));
    }

    @ApiOperation(value = "查询实体信息(所有)")
    @RequestMapping(value = "/getEntities", method = RequestMethod.GET)
    public TeslaBaseResult getEntities(){
        return buildSucceedResult(entityMetaService.getEntities());
    }

    @ApiOperation(value = "查询实体列信息(根据实体ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getFieldsByEntityId", method = RequestMethod.GET)
    public TeslaBaseResult getFieldsByEntityId(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(entityMetaService.getFieldsByEntityId(id));
    }

    @ApiOperation(value = "查询实体列信息(根据实体名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "实体名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getFieldsByEntityName", method = RequestMethod.GET)
    public TeslaBaseResult getFieldsByEntityName(@RequestParam(name = "name") String name) throws Exception {
        return buildSucceedResult(entityMetaService.getFieldsByEntityName(name));
    }

    @ApiOperation(value = "查询实体和列信息(根据实体ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/getEntityWithFieldsById", method = RequestMethod.GET)
    public TeslaBaseResult getEntityWithFieldsById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(entityMetaService.getEntityWithFieldsById(id));
    }

    @ApiOperation(value = "查询实体和列信息(根据实体名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "实体名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/getEntityWithFieldsByName", method = RequestMethod.GET)
    public TeslaBaseResult getEntityWithFieldsByName(@RequestParam(name = "name") String name) {
        return buildSucceedResult(entityMetaService.getEntityWithFieldsByName(name));
    }

    @ApiOperation(value = "删除实体(根据实体ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/deleteEntityById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteEntityById(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(entityMetaService.deleteEntityById(id));
    }

    @ApiOperation(value = "删除实体(根据实体名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "实体名称", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/deleteEntityByName", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteEntityByName(@RequestParam(name = "name") String name) throws Exception {
        return buildSucceedResult(entityMetaService.deleteEntityByName(name));
    }

    @ApiOperation(value = "删除实体列(根据实体ID和列ID,仅删除列定义)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "entityId", value = "实体ID", defaultValue = "0", paramType = "query"),
            @ApiImplicitParam(name = "fieldId", value = "列ID", defaultValue = "1", paramType = "query")
    })
    @RequestMapping(value = "/deleteFieldById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteFieldById(@RequestParam(name = "entityId") Long entityId,
                                                 @RequestParam(name = "fieldId") Long fieldId) throws Exception {
        return buildSucceedResult(entityMetaService.deleteFieldById(entityId, fieldId));
    }

    @ApiOperation(value = "删除实体列(根据实体ID和列名,仅删除列定义)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "entityId", value = "实体ID", defaultValue = "0", paramType = "query"),
            @ApiImplicitParam(name = "fieldName", value = "列名", defaultValue = "APP", paramType = "query")
    })
    @RequestMapping(value = "/deleteFieldByName", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteFieldByName(@RequestParam(name = "entityId") Long entityId,
                                                 @RequestParam(name = "fieldName") String fieldName) throws Exception {
        return buildSucceedResult(entityMetaService.deleteFieldByName(entityId, fieldName));
    }


    @ApiOperation(value = "创建实体")
    @RequestMapping(value = "/createEntity", method = RequestMethod.POST)
    public TeslaBaseResult createEntity(@RequestBody EntityCreateReq req) throws Exception {
        return buildSucceedResult(entityMetaService.createEntity(req));
    }

    @ApiOperation(value = "创建实体(带列信息)")
    @RequestMapping(value = "/createEntityWithFields", method = RequestMethod.POST)
    public TeslaBaseResult createEntityWithFields(@RequestBody EntityWithFieldsCreateReq req) throws Exception {
        return buildSucceedResult(entityMetaService.createEntityWithFields(req.getMetaReq(), Arrays.asList(req.getFieldsReq())));
    }

    @ApiOperation(value = "更新实体")
    @RequestMapping(value = "/updateEntity", method = RequestMethod.POST)
    public TeslaBaseResult updateEntity(@RequestBody EntityUpdateReq req) throws Exception {
        return buildSucceedResult(entityMetaService.updateEntity(req));
    }

    @ApiOperation(value = "新增列(根据实体ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "entityId", value = "实体ID", defaultValue = "0", paramType = "query")
    })
    @RequestMapping(value = "/addFieldByEntityId", method = RequestMethod.POST)
    public TeslaBaseResult addFieldByEntityId(@RequestParam(name = "entityId") Long entityId,
                                              @RequestBody @ApiParam(value = "实体列") EntityFieldCreateReq fieldReq) throws Exception {
        entityMetaService.addFieldByEntityId(entityId, fieldReq);
        return buildSucceedResult("done");
    }

    @ApiOperation(value = "更新列(根据实体ID)")
    @RequestMapping(value = "/updateFieldByEntityId", method = RequestMethod.POST)
    public TeslaBaseResult updateFieldByEntityId(@RequestParam(name = "entityId") Long entityId,
                                              @RequestBody @ApiParam(value = "实体列") EntityFieldUpdateReq fieldReq) throws Exception {
        entityMetaService.updateFieldByEntityId(entityId, fieldReq);
        return buildSucceedResult("done");
    }
}
