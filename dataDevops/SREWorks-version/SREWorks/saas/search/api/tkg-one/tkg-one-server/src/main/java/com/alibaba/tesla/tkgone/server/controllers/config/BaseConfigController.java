package com.alibaba.tesla.tkgone.server.controllers.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.web.controller.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author jialiang.tjl
 */
@RestController
@RequestMapping("/config/base")
public class BaseConfigController extends BaseController {

    @Autowired
    BaseConfigService baseConfigService;

    @RequestMapping(value = "/delCategoryTypeIdNameString", method = RequestMethod.DELETE)
    public TeslaBaseResult delCategoryTypeIdNameString(String category, String type, String id, String name)
            throws Exception {

        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        if (StringUtils.isEmpty(type)) {
            type = Constant.DEFAULT_NR_TYPE;
        }
        if (StringUtils.isEmpty(id)) {
            id = Constant.DEFAULT_NR_ID;
        }
        if (StringUtils.isEmpty(name)) {
            throw new Exception("name值不能为空");
        }
        return buildSucceedResult(baseConfigService
                .delete(ConfigDto.builder().category(category).nrType(type).nrId(id).name(name).build()));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public TeslaBaseResult delCategoryById(@PathVariable Long id) throws Exception {
        if (null == id) {
            throw new Exception("id值不能为空");
        }
        return buildSucceedResult(baseConfigService.deleteById(id));
    }

    @RequestMapping(value = "/setCategoryTypeIdNameString", method = RequestMethod.POST)
    public TeslaBaseResult setCategoryTypeIdNameString(String category, String type, String id, String name,
            @RequestBody Object content) {

        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        if (StringUtils.isEmpty(type)) {
            type = Constant.DEFAULT_NR_TYPE;
        }
        if (StringUtils.isEmpty(id)) {
            id = Constant.DEFAULT_NR_ID;
        }
        String contentString = Tools.objectToString(content);
        return buildSucceedResult(baseConfigService.setCategoryTypeIdNameContent(category, type, id, name,
                contentString, getUserEmployeeId()));
    }

    @RequestMapping(value = "/upsertConfig", method = RequestMethod.PUT)
    public TeslaBaseResult updateConfig(@RequestBody JSONObject configObj) {
        String category = configObj.getString("category");
        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        String type = configObj.getString("nrType");
        if (StringUtils.isEmpty(type)) {
            type = Constant.DEFAULT_NR_TYPE;
        }
        String id = configObj.getString("nrId");
        if (StringUtils.isEmpty(id)) {
            id = Constant.DEFAULT_NR_ID;
        }
        Object content = configObj.getObject("content", Object.class);
        String contentString = Tools.objectToString(content);
        return buildSucceedResult(baseConfigService.setCategoryTypeIdNameContent(category, type, id,
                configObj.getString("name"), contentString, getUserEmployeeId()));
    }

    @RequestMapping(value = "/getCategoryTypeIdNameString", method = RequestMethod.GET)
    public TeslaBaseResult getCategoryTypeIdNameString(String category, String type, String id, String name) {

        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        if (StringUtils.isEmpty(type)) {
            type = Constant.DEFAULT_NR_TYPE;
        }
        if (StringUtils.isEmpty(id)) {
            id = Constant.DEFAULT_NR_ID;
        }

        String content = baseConfigService.getCategoryTypeIdNameContent(category, type, id, name);
        Object retData;
        try {
            retData = JSONObject.parse(content);
        } catch (Exception e) {
            retData = content;
        }
        return buildSucceedResult(retData);
    }

    @RequestMapping(value = "/getCategoryTypeIdNameConfigList", method = RequestMethod.GET)
    public TeslaBaseResult getCategoryTypeIdNameConfigList(String category, String type, String id, String name) {

        List<ConfigDto> configDtoList = baseConfigService
                .getConfigDtoList(ConfigDto.builder().category(category).nrType(type).nrId(id).name(name).build());
        return buildSucceedResult(configDtoList);
    }

    @RequestMapping(value = "/getEntityIndices", method = RequestMethod.GET)
    public TeslaBaseResult getEntityIndices() {
        String name = "categoryIncludeIndexes";
        List<ConfigDto> configDtos = baseConfigService.getConfigDtoList(ConfigDto.builder().name(name).build());

        HashSet<String> indices = new HashSet<>();
        for (ConfigDto configDto : configDtos) {
            String content = configDto.getContent();
            indices.addAll(JSONArray.parseArray(content).toJavaList(String.class));
        }
        List<String> result = new ArrayList<>(indices);
        Collections.sort(result);
        return buildSucceedResult(result);
    }
}
