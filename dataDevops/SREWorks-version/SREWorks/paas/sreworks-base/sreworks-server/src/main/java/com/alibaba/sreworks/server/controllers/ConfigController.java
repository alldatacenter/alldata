package com.alibaba.sreworks.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.Config;
import com.alibaba.sreworks.server.services.ConfigService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;


/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/config")
@Api(tags = "插件-集群")
public class ConfigController extends BaseController {

    @Autowired
    ConfigService configService;

    @ApiOperation(value = "系统设置")
    @RequestMapping(value = "systemConfig", method = RequestMethod.GET)
    public TeslaBaseResult systemConfig() {
        List<Config> configList = configService.getAll();
        return buildSucceedResult(
            configList.stream()
                .collect(Collectors.toMap(Config::getName, Config::getContent))
        );
    }

    /**
     * 检查git配置有效性
     * @return
     */
    @ApiOperation(value = "checkGitConfig")
    @RequestMapping(value = "checkGitConfig", method = RequestMethod.POST)
    public TeslaBaseResult checkGitConfig(@RequestBody JSONObject jsonObject) {
        String repoDomain = jsonObject.getString("repoDomain");
        String repoGroup = jsonObject.getString("repoGroup");
        String token = jsonObject.getString("token");

        GitLabApi api;
        JSONObject result = new JSONObject();
        result.put("result", "ERROR");

        try {
            api = new GitLabApi(repoDomain, token);
        } catch (Exception e) {
            result.put("message", "Gitlab服务检查失败,无法使用自动创建仓库能力");
            return buildSucceedResult(result);
        }

        Group group;
        try {
            group = api.getGroupApi().getGroup(repoGroup);
        } catch (GitLabApiException e) {
            result.put("message", "检查Gitlab Group失败，请检查该Gitlab Group是否存在");
            return buildSucceedResult(result);
        }

        result.put("result", "OK");
        result.put("repoDomain", repoDomain);
        result.put("repoGroup", repoDomain);
        result.put("message", "Gitlab服务检查成功, 可以使用自动创建仓库能力");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "set")
    @RequestMapping(value = "set", method = RequestMethod.POST)
    public TeslaBaseResult set(@RequestBody JSONObject jsonObject) {
        for (String name : jsonObject.keySet()) {
            String content = jsonObject.getString(name);
            configService.set(name, content, getUserEmployeeId());
        }
        JSONObject result = new JSONObject();
        result.put("result", "OK");
        result.put("data", jsonObject);
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(String name) {
        return buildSucceedResult(configService.get(name));
    }

    @ApiOperation(value = "del")
    @RequestMapping(value = "del", method = RequestMethod.DELETE)
    public TeslaBaseResult del(String name) {
        return buildSucceedResult(configService.del(name));
    }

    @ApiOperation(value = "setObject")
    @RequestMapping(value = "setObject", method = RequestMethod.POST)
    public TeslaBaseResult setObject(String name, @RequestBody JSONObject content) {
        configService.set(name, JSONObject.toJSONString(content), getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "getObject")
    @RequestMapping(value = "getObject", method = RequestMethod.GET)
    public TeslaBaseResult getObject(String name) {

        return buildSucceedResult(
            JSONObject.parse(configService.get(name))
        );

    }
}
