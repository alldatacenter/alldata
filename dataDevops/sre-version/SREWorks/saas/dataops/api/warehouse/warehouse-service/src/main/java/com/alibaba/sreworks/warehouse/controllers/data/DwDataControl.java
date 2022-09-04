package com.alibaba.sreworks.warehouse.controllers.data;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.sreworks.warehouse.common.exception.ParamException;
import com.alibaba.sreworks.warehouse.services.entity.EntityDataServiceImpl;
import com.alibaba.sreworks.warehouse.services.model.ModelDataServiceImpl;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 模型数据Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/dw/data/")
@Api(tags = "数据接口")
public class DwDataControl extends BaseController {

    @Autowired
    ModelDataServiceImpl modelDataService;

    @Autowired
    EntityDataServiceImpl entityDataService;


    @ApiOperation(value = "单条数据写入(需要预先定义模型)")
    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public TeslaBaseResult push(@RequestParam(name = "id") Long id, @RequestParam(name = "type") String type,
                                          @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        if (StringUtils.isEmpty(type) || !Constant.DW_TYPES.contains(type)) {
            throw new ParamException(String.format("数据类型错误, 合法类型%s", Constant.DW_TYPES));
        }
        if (type.equals(Constant.DW_ENTITY)) {
            return buildSucceedResult(entityDataService.flushDwData(id, node));
        } else {
            return buildSucceedResult(modelDataService.flushDwData(id, node));
        }
    }

    @ApiOperation(value = "多条数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushBatch", method = RequestMethod.POST)
    public TeslaBaseResult pushBatch(@RequestParam(name = "id") Long id, @RequestParam(name = "type") String type,
                                           @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        if (StringUtils.isEmpty(type) || !Constant.DW_TYPES.contains(type)) {
            throw new ParamException(String.format("数据类型错误, 合法类型%s", Constant.DW_TYPES));
        }
        if (type.equals(Constant.DW_ENTITY)) {
            return buildSucceedResult(entityDataService.flushDwDatas(id, Arrays.asList(nodes)));
        } else {
            return buildSucceedResult(modelDataService.flushDwDatas(id, Arrays.asList(nodes)));
        }
    }

    @ApiOperation(value = "单条数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushByName", method = RequestMethod.POST)
    public TeslaBaseResult pushByName(@RequestParam(name = "name") String name, @RequestParam(name = "type") String type,
                                    @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        if (StringUtils.isEmpty(type) || !Constant.DW_TYPES.contains(type)) {
            throw new ParamException(String.format("数据类型错误, 合法类型%s", Constant.DW_TYPES));
        }
        if (type.equals(Constant.DW_ENTITY)) {
            return buildSucceedResult(entityDataService.flushDwData(name, node));
        } else {
            return buildSucceedResult(modelDataService.flushDwData(name, node));
        }
    }

    @ApiOperation(value = "多条数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushBatchByName", method = RequestMethod.POST)
    public TeslaBaseResult pushBatchByName(@RequestParam(name = "name") String name, @RequestParam(name = "type") String type,
                                           @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        if (StringUtils.isEmpty(type) || !Constant.DW_TYPES.contains(type)) {
            throw new ParamException(String.format("数据类型错误, 合法类型%s", Constant.DW_TYPES));
        }
        if (type.equals(Constant.DW_ENTITY)) {
            return buildSucceedResult(entityDataService.flushDwDatas(name, Arrays.asList(nodes)));
        } else {
            return buildSucceedResult(modelDataService.flushDwDatas(name, Arrays.asList(nodes)));
        }
    }



    @ApiOperation(value = "单条实体数据写入(需要预先定义实体)")
    @RequestMapping(value = "/pushEntityData", method = RequestMethod.POST)
    public TeslaBaseResult pushEntityData(@RequestParam(name = "entityId") Long entityId,
                                          @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        return buildSucceedResult(entityDataService.flushDwData(entityId, node));
    }

    @ApiOperation(value = "多条实体数据写入(需要预先定义实体)")
    @RequestMapping(value = "/pushEntityDatas", method = RequestMethod.POST)
    public TeslaBaseResult pushEntityDatas(@RequestParam(name = "entityId") Long entityId,
                                           @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        return buildSucceedResult(entityDataService.flushDwDatas(entityId, Arrays.asList(nodes)));
    }

    @ApiOperation(value = "单条模型数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushModelData", method = RequestMethod.POST)
    public TeslaBaseResult pushModelData(@RequestParam(name = "modelId") Long modelId,
                                         @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        return buildSucceedResult(modelDataService.flushDwData(modelId, node));
    }

    @ApiOperation(value = "多条模型数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushModelDatas", method = RequestMethod.POST)
    public TeslaBaseResult pushModelDatas(@RequestParam(name = "modelId") Long modelId,
                                          @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        return buildSucceedResult(modelDataService.flushDwDatas(modelId, Arrays.asList(nodes)));
    }

    @ApiOperation(value = "单条实体数据写入(需要预先定义实体)")
    @RequestMapping(value = "/pushEntityDataByName", method = RequestMethod.POST)
    public TeslaBaseResult pushEntityDataByName(@RequestParam(name = "entityName") String entityName,
                                          @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        return buildSucceedResult(entityDataService.flushDwData(entityName, node));
    }

    @ApiOperation(value = "多条实体数据写入(需要预先定义实体)")
    @RequestMapping(value = "/pushEntityDatasByName", method = RequestMethod.POST)
    public TeslaBaseResult pushEntityDatasByName(@RequestParam(name = "entityName") String entityName,
                                           @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        return buildSucceedResult(entityDataService.flushDwDatas(entityName, Arrays.asList(nodes)));
    }

    @ApiOperation(value = "单条模型数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushModelDataByName", method = RequestMethod.POST)
    public TeslaBaseResult pushModelDataByName(@RequestParam(name = "modelName") String modelName,
                                         @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
        return buildSucceedResult(modelDataService.flushDwData(modelName, node));
    }

    @ApiOperation(value = "多条模型数据写入(需要预先定义模型)")
    @RequestMapping(value = "/pushModelDatasByName", method = RequestMethod.POST)
    public TeslaBaseResult pushModelDatasByName(@RequestParam(name = "modelName") String modelName,
                                          @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
        return buildSucceedResult(modelDataService.flushDwDatas(modelName, Arrays.asList(nodes)));
    }
}
