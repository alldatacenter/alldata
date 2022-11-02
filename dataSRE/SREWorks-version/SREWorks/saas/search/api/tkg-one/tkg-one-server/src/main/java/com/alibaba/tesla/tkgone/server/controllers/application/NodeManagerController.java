package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.services.app.NodeManagerService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangjinghua
 */
@RestController
@RequestMapping("/nodeManager")
public class NodeManagerController extends BaseController {

    @Autowired
    private NodeManagerService nodeManagerService;

    @Autowired
    RedisHelper redisHelper;

    @RequestMapping(value = "/getTypeList", method = RequestMethod.GET)
    public TeslaBaseResult getTypeList(String appName) {
        return buildSucceedResult(nodeManagerService.getTypeList(appName));
    }

    @RequestMapping(value = "/setTypeList", method = RequestMethod.POST)
    public TeslaBaseResult setTypeList(String appName, @RequestBody JSONArray typeList) {
        nodeManagerService.setTypeList(appName, typeList, getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/delType", method = RequestMethod.DELETE)
    public TeslaBaseResult delType(String appName, String type) {
        nodeManagerService.delType(appName, type, getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/createType", method = RequestMethod.POST)
    public TeslaBaseResult createType(@RequestBody JSONObject jsonObject) throws Exception {
        nodeManagerService.createType(jsonObject.getString("appName"), jsonObject.getString("type"),
            jsonObject.getString("alias"), jsonObject.getString("description"), getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/updateTypeAlias", method = RequestMethod.POST)
    public TeslaBaseResult updateTypeAlias(@RequestBody JSONObject jsonObject) {
        nodeManagerService.updateTypeAlias(jsonObject.getString("appName"), jsonObject.getString("type"),
            jsonObject.getString("alias"), getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/updateTypeDescription", method = RequestMethod.POST)
    public TeslaBaseResult updateTypeDescription(@RequestBody JSONObject jsonObject) {
        nodeManagerService.updateTypeDescription(jsonObject.getString("appName"), jsonObject.getString("type"),
            jsonObject.getString("description"), getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/updateType", method = RequestMethod.POST)
    public TeslaBaseResult updateType(@RequestBody JSONObject jsonObject) {
        nodeManagerService.updateType(jsonObject.getString("appName"), jsonObject.getString("type"),
            jsonObject.getString("alias"), jsonObject.getString("description"), getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @RequestMapping(value = "/getTypeMetaInfo", method = RequestMethod.GET)
    public TeslaBaseResult getTypeMetaInfo(String appName, String type) {
        return buildSucceedResult(nodeManagerService.getTypeMetaInfo(appName, type));
    }

    @RequestMapping(value = "/getTypeMetaInfos", method = RequestMethod.GET)
    public TeslaBaseResult getTypeMetaInfos(String appName) {
        return buildSucceedResult(nodeManagerService.getTypeMetaInfos(appName));
    }

    @RequestMapping(value = "/getPropertiesMapping", method = RequestMethod.GET)
    public TeslaBaseResult getPropertiesMapping(String type) {
        return buildSucceedResult(nodeManagerService.getPropertiesMapping(type));
    }

    @RequestMapping(value = "/setPropertiesMapping", method = RequestMethod.POST)
    public TeslaBaseResult setPropertiesMapping(String type, @RequestBody JSONObject propertiesMapping)
        throws Exception {
        return buildSucceedResult(
            nodeManagerService.setPropertiesMapping(type, propertiesMapping, getUserEmployeeId())
        );
    }

    @RequestMapping(value = "/getProgress", method = RequestMethod.GET)
    public TeslaBaseResult getProgress(String uuid) {
        JSONObject retJson = new JSONObject();
        retJson.put("content", redisHelper.hget(Constant.REDIS_REINDEX_STATUS, uuid));
        retJson.put("progress", redisHelper.hget(Constant.REDIS_REINDEX_STATUS, uuid + "_progress"));
        return buildSucceedResult(retJson);
    }

    @RequestMapping(value = "/getLastProgress", method = RequestMethod.GET)
    public TeslaBaseResult getLastProgress(String type) {
        return buildSucceedResult(redisHelper.hget(Constant.REDIS_REINDEX_NEWEST_UUID, type));
    }

}
