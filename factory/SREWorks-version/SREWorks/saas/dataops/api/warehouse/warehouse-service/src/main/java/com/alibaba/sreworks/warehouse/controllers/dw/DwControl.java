package com.alibaba.sreworks.warehouse.controllers.dw;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.entity.EntityMetaService;
import com.alibaba.sreworks.warehouse.api.model.ModelMetaService;
import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数仓元Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/dw/meta/")
@Api(tags = "数仓--元数据")
public class DwControl extends BaseController {

    @Autowired
    EntityMetaService entityMetaService;

    @Autowired
    ModelMetaService modelMetaService;

    @ApiOperation(value = "查询数仓元信息(所有)")
    @RequestMapping(value = "/getMetas", method = RequestMethod.GET)
    public TeslaBaseResult getEntities(){
        List<JSONObject> entities = entityMetaService.getEntities();
        List<JSONObject> models = modelMetaService.getModels();
        JSONObject result = new JSONObject();
        result.put(Constant.DW_ENTITY, entities);
        result.put(Constant.DW_MODEL, models);
        return buildSucceedResult(result);
    }
}
