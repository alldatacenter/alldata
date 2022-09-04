package com.alibaba.tesla.tkgone.server.controllers.consumer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.consumer.ConsumerSourceType;
import com.alibaba.tesla.tkgone.server.domain.Consumer;
import com.alibaba.tesla.tkgone.server.domain.ConsumerExample;
import com.alibaba.tesla.tkgone.server.domain.ConsumerMapper;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jialiang.tjl
 */
@RestController
@RequestMapping("/consumer")
public class ConsumerController extends BaseController {

    @Autowired
    ConsumerMapper consumerMapper;

    @RequestMapping(value = "/listAll", method = RequestMethod.GET)
    public TeslaBaseResult listAll() {
        return buildSucceedResult(consumerMapper.selectByExample(new ConsumerExample()));
    }

    @RequestMapping(value = "/listName", method = RequestMethod.GET)
    public TeslaBaseResult listName() {
        return buildSucceedResult(consumerMapper.selectByExample(new ConsumerExample()).stream().map(Consumer::getName)
                .collect(Collectors.toList()));
    }

    @RequestMapping(value = "/listInvalidName", method = RequestMethod.GET)
    public TeslaBaseResult listInvalidName() {
        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andEnableEqualTo("true");
        return buildSucceedResult(consumerMapper.selectByExample(consumerExample).stream().map(Consumer::getName)
                .collect(Collectors.toList()));
    }

    @RequestMapping(value = "/listMetaByAppName", method = RequestMethod.GET)
    public TeslaBaseResult listMetaByAppName(String appName) {
        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andAppNameEqualTo(appName);
        return buildSucceedResult(consumerMapper.selectByExample(consumerExample).stream().peek(consumer -> {
            consumer.setUserImportConfig(null);
            consumer.setImportConfig(null);
        }).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/listByType", method = RequestMethod.GET)
    public TeslaBaseResult listByType(String type) {
        JSONObject retJson = new JSONObject();
        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andImportConfigLike("%\"__type\"%:%\"" + type + "\"%");
        List<ConsumerDto> consumerDtoList = consumerMapper.selectByExample(consumerExample).stream()
                .map(ConsumerDto::new).collect(Collectors.toList());
        for (ConsumerDto consumerDto : consumerDtoList) {
            JSONObject tmpJson = new JSONObject();
            retJson.put(consumerDto.getName(), tmpJson);
            for (JSONArray importConfig : consumerDto.getImportConfigArray().toJavaList(JSONArray.class)) {
                for (JSONObject importJson : importConfig.toJavaList(JSONObject.class)) {
                    if (type.equals(importJson.getString(Constant.INNER_TYPE))) {
                        tmpJson.putAll(importJson);
                    }
                }
            }
        }
        return buildSucceedResult(retJson);
    }

    @RequestMapping(value = "listBySource", method = RequestMethod.GET)
    public TeslaBaseResult listBySource(String sourceType, String groupName, String repoName) throws Exception {
        if (sourceType == null) {
            throw new Exception("source type不能为null。");
        }

        ConsumerExample consumerExample = new ConsumerExample();
        ConsumerExample.Criteria criteria = consumerExample.createCriteria();
        if (groupName != null && !groupName.isEmpty()) {
            criteria.andSourceInfoLike("%\"groupName\"%:%\"" + groupName + "\"%");
        }
        if (repoName != null && !repoName.isEmpty()) {
            criteria.andSourceInfoLike("%\"repoName\"%:%\"" + repoName + "\"%");
        }
        // 开始考虑将xAuthToken放在请求参数中，出于安全还是不把xAuthToken放在请求参数中了。

        consumerExample.createCriteria().andSourceTypeEqualTo(sourceType);
        return buildSucceedResult(consumerMapper.selectByExample(consumerExample).stream().peek(consumer -> {
            consumer.setUserImportConfig(null);
            consumer.setImportConfig(null);
        }).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/getByName", method = RequestMethod.GET)
    public TeslaBaseResult getByName(String name) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        List<Consumer> consumerList = consumerMapper.selectByExample(consumerExample);
        if (consumerList.isEmpty()) {
            return buildSucceedResult(null);
        }
        ConsumerDto consumerDto = consumerList.stream().map(ConsumerDto::new).collect(Collectors.toList()).get(0);
        JSONObject retJson = (JSONObject) JSONObject.toJSON(consumerDto);
        retJson.remove("importConfig");
        retJson.remove("userImportConfig");
        retJson.remove("sourceInfo");
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/deleteByName", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteByName(String name) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        return buildSucceedResult(consumerMapper.deleteByExample(consumerExample));

    }

    @RequestMapping(value = "/deleteById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteById(long id) {
        return buildSucceedResult(consumerMapper.deleteByPrimaryKey(id));

    }

    @RequestMapping(value = "/updateByName", method = RequestMethod.POST, consumes = "application/json")
    public TeslaBaseResult updateByName(String name, @RequestBody ConsumerDto consumerDto) {
        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        List<Consumer> consumerList = consumerMapper.selectByExample(consumerExample);

        JSONObject retJson = new JSONObject();
        retJson.put("deleteNum", deleteByName(name));

        consumerDto.setCreator(getUserEmployeeId());
        consumerDto.setModifier(getUserEmployeeId());
        consumerDto.setGmtModified(new Date());
        consumerDto.setGmtCreate(new Date());
        consumerDto.setId(null);
        if (!CollectionUtils.isEmpty(consumerList) && consumerDto.getOffset() == null) {
            consumerDto.setOffset(consumerList.get(0).getOffset());
        }

        consumerDto.setModifier(getUserEmployeeId());
        consumerDto.setGmtCreate(new Date());
        consumerDto.setGmtModified(new Date());
        retJson.put("insertNum", consumerMapper.insert(consumerDto.toConsumer()));
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/updateScriptContentByName", method = RequestMethod.POST, consumes = "application/json")
    public TeslaBaseResult updateByName(String name, @RequestBody String content) throws Exception {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        List<Consumer> consumerList = consumerMapper.selectByExample(consumerExample);
        if (consumerList.size() != 1) {
            throw new Exception("不存在该名称的导入配置");
        }
        if (!ConsumerSourceType.script.toString().equals(consumerList.get(0).getSourceType())) {
            throw new Exception("该名称的导入不是一个脚本导入");
        }

        JSONObject retJson = new JSONObject();
        retJson.put("deleteNum", deleteByName(name));

        ConsumerDto consumerDto = new ConsumerDto(consumerList.get(0));

        consumerDto.setModifier(getUserEmployeeId());
        consumerDto.setGmtModified(new Date());
        consumerDto.setId(null);
        consumerDto.getSourceInfoJson().put("content", content);

        retJson.put("insertNum", consumerMapper.insert(consumerDto.toConsumer()));
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/setEnable/{name}/{enable}", method = RequestMethod.PUT)
    public TeslaBaseResult setEnable(@PathVariable String name, @PathVariable Boolean enable) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.setEnable(enable.toString());
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setOffset/{name}/{offset}", method = RequestMethod.PUT)
    public TeslaBaseResult setOffset(@PathVariable String name, @PathVariable String offset) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.setOffset(offset);
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setStartPeriod/{name}/{startPeriod}", method = RequestMethod.PUT)
    public TeslaBaseResult setStartPeriod(@PathVariable String name, @PathVariable Integer startPeriod) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("startPeriod", startPeriod);
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setInterval/{name}/{interval}", method = RequestMethod.PUT)
    public TeslaBaseResult setInterval(@PathVariable String name, @PathVariable Integer interval) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("interval", interval);
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setCronExpression", method = RequestMethod.POST)
    public TeslaBaseResult setCronExpression(String name, @RequestBody String cronExpression) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("cronExpression", cronExpression);
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setIsPartition/{name}/{isPartition}", method = RequestMethod.PUT)
    public TeslaBaseResult setIsPartition(@PathVariable String name, @PathVariable Boolean isPartition) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("isPartition", isPartition);
        return updateByName(name, consumerDto);

    }

    @RequestMapping(value = "/setLockClient/{name}/{lockClient}", method = RequestMethod.PUT)
    public TeslaBaseResult setLockClient(@PathVariable String name, @PathVariable String lockClient) {
        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("lockClient", lockClient);
        return updateByName(name, consumerDto);
    }

    @RequestMapping(value = "/setTimeout/{name}/{timeout}", method = RequestMethod.PUT)
    public TeslaBaseResult setTimeout(@PathVariable String name, @PathVariable Integer timeout) {

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andNameEqualTo(name);
        ConsumerDto consumerDto = new ConsumerDto(consumerMapper.selectByExample(consumerExample).get(0));
        consumerDto.getSourceInfoJson().put("timeout", timeout);
        return updateByName(name, consumerDto);

    }

}
