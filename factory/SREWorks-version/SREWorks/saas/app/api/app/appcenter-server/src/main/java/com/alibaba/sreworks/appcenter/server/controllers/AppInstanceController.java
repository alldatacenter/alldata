package com.alibaba.sreworks.appcenter.server.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appcenter.server.params.AppInstanceUpdateParam;
import com.alibaba.sreworks.appcenter.server.params.AppInstanceUpdateResourceParam;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DTO.AppComponentInstanceDetail;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.domain.utils.AppUtil;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerAppInstanceService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerAppService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/appcenter/appInstance")
@Api(tags = "应用实例")
public class AppInstanceController extends BaseController {

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    FlyadminAppmanagerAppInstanceService flyadminAppmanagerAppInstanceService;

    @Autowired
    FlyadminAppmanagerAppService flyadminAppmanagerAppService;

    //@Autowired
    //AppUpdateService appUpdateService;

    @Autowired
    AppRepository appRepository;

    @Autowired
    FlyadminAuthproxyUserService flyadminAuthproxyUserService;

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    private List<JSONObject> filterRet(List<JSONObject> ret, String labels) {
        if (StringUtils.isEmpty(labels) || !labels.contains("=")) {
            return ret;
        }
        Stream<JSONObject> stream = ret.stream();
        for (String label : labels.split(",")) {
            if (!label.contains("=")) {
                continue;
            }
            String[] words = label.split("=");
            if (words.length != 2) {
                continue;
            }
            stream = stream.filter(jsonObject -> {
                JSONObject appLabels = JSONObject.parseObject(jsonObject.getString("appLabels"));
                return appLabels != null && Objects.equals(appLabels.getString(words[0]), words[1]);
            });
        }
        return stream.collect(Collectors.toList());
    }

    @ApiOperation(value = "我的列表")
    @RequestMapping(value = "listMy", method = RequestMethod.GET)
    public TeslaBaseResult listMy(String name) {
        name = StringUtils.isEmpty(name) ? "" : name;
        name = "%" + name + "%";
        List<JSONObject> ret = appInstanceRepository.findObjectByUser(getUserEmployeeId(), name);
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "listAppIdDevPre")
    @RequestMapping(value = "listAppIdDevPre", method = RequestMethod.GET)
    public TeslaBaseResult listAppIdDevPre(Long appId, String name, String labels) {
        name = StringUtils.isEmpty(name) ? "" : name;
        name = "%" + name + "%";
        List<JSONObject> ret = appInstanceRepository.findObjectByUser(getUserEmployeeId(), name);
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        ret = ret.stream()
            .filter(x -> Arrays.asList("dev", "pre").contains(x.getString("stageId")))
            .filter(x -> Objects.equals(x.getLong("appId"), appId))
            .collect(Collectors.toList());
        ret = filterRet(ret, labels);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "allAppInstances")
    @RequestMapping(value = "allAppInstances", method = RequestMethod.GET)
    public TeslaBaseResult allAppInstances(Long page, Long pageSize) throws Exception {
        if (page == null || page == 0L) {
            page = 1L;
        }
        if (pageSize == null || pageSize == 0L) {
            pageSize = 10L;
        }
        List<JSONObject> clusters = clusterRepository.findObject();
        Map<String, JSONObject> clusterMap = new HashMap<>();
        for (JSONObject cluster : clusters) {
            clusterMap.put(cluster.getString("id"), cluster);
        }
        String clusterIds = clusterMap.values().stream().map(x->x.getString("id") + "id").collect(Collectors.joining(","));

        JSONObject ret = flyadminAppmanagerAppInstanceService.list("prod",clusterIds,"",page,pageSize, "app");
        for (JSONObject item : ret.getJSONArray("items").toJavaList(JSONObject.class)) {
            String clusterId = item.getString("clusterId").replace("id","");
            JSONObject cluster = clusterMap.get(clusterId);
            if(cluster != null) {
                item.put("clusterDetail", JsonUtil.map(
                        "cluster_name", cluster.getString("cluster_name"),
                        "account_type", cluster.getString("account_type"),
                        "name", cluster.getString("name"),
                        "id", cluster.getInteger("id")
                ));
                item.put("team", JsonUtil.map(
                        "team_id", cluster.getInteger("team_id"),
                        "team_name", cluster.getString("team_name")
                ));
            }
        }
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "appInstances")
    @RequestMapping(value = "appInstances", method = RequestMethod.GET)
    public TeslaBaseResult appInstances(String name, Long page, Long pageSize) throws Exception {
        if (page == null || page == 0L) {
            page = 1L;
        }
        if (pageSize == null || pageSize == 0L) {
            pageSize = 10L;
        }
        List<JSONObject> clusters = clusterRepository.findObjectByUserAndNameLike(getUserEmployeeId(), "%%");
        Map<String, JSONObject> clusterMap = clusters.stream().collect(Collectors.toMap(x -> x.getString("id"), x -> x));
        String clusterIds = clusters.stream().map(x->x.getString("id") + "id").collect(Collectors.joining(","));

        JSONObject ret = flyadminAppmanagerAppInstanceService.list("prod",clusterIds,"",page,pageSize,"app");
        for (JSONObject item : ret.getJSONArray("items").toJavaList(JSONObject.class)) {
            String clusterId = item.getString("clusterId").replace("id","");
            JSONObject cluster = clusterMap.get(clusterId);
            if(cluster != null) {
                item.put("clusterDetail", JsonUtil.map(
                        "cluster_name", cluster.getString("cluster_name"),
                        "account_type", cluster.getString("account_type"),
                        "name", cluster.getString("name"),
                        "id", cluster.getInteger("id")
                ));
                item.put("team", JsonUtil.map(
                        "team_id", cluster.getInteger("team_id"),
                        "team_name", cluster.getString("team_name")
                ));
            }
        }
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "listMyProd")
    @RequestMapping(value = "listMyProd", method = RequestMethod.GET)
    public TeslaBaseResult listMyProd(String name, String labels) {
        name = StringUtils.isEmpty(name) ? "" : name;
        name = "%" + name + "%";
        List<JSONObject> ret = appInstanceRepository.findObjectByUser(getUserEmployeeId(), name);
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        ret = ret.stream()
            .filter(x -> Objects.equals("prod", x.getString("stageId")))
            .collect(Collectors.toList());
        ret = filterRet(ret, labels);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "公共列表")
    @RequestMapping(value = "listPublic", method = RequestMethod.GET)
    public TeslaBaseResult listPublic(String name) {
        name = StringUtils.isEmpty(name) ? "" : name;
        name = "%" + name + "%";
        List<JSONObject> ret = appInstanceRepository.findPublicObject(name);
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        ret = ret.stream()
            .filter(x -> Objects.equals("prod", x.getString("stageId")))
            .collect(Collectors.toList());
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) throws IOException, ApiException {
        JSONObject ret = appInstanceRepository.getObjectById(id);
        Map<String, String> map = flyadminAuthproxyUserService.userEmpIdNameMap(getUserEmployeeId());
        ret.put("creator_name", map.get(ret.getString("creator")));
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "getByInstanceId")
    @RequestMapping(value = "getByInstanceId", method = RequestMethod.GET)
    public TeslaBaseResult get(String instanceId) throws Exception {
        JSONObject instanceInfo = flyadminAppmanagerAppInstanceService.get(instanceId);
        try {
            JSONObject dataopsInfo = flyadminAppmanagerAppService.getByAppmanagerId("dataops");
            instanceInfo.put("dataopsInfo", dataopsInfo);
        } catch (Exception e) {
            instanceInfo.put("dataopsInfo", null);
        }
        return buildSucceedResult(instanceInfo);
    }

    @ApiOperation(value = "getResource")
    @RequestMapping(value = "getResource", method = RequestMethod.GET)
    public TeslaBaseResult getResource(Long id) throws JsonProcessingException {
        AppInstance appInstance = appInstanceRepository.findFirstById(id);
        return buildSucceedResult(YamlUtil.toYaml(appInstance.detail().getResource().toJsonObject()));
    }
    //
    //@ApiOperation(value = "updateResource")
    //@RequestMapping(value = "updateResource", method = RequestMethod.POST)
    //public TeslaBaseResult updateResource(Long id, @RequestBody AppInstanceUpdateResourceParam param)
    //    throws IOException, ApiException {
    //    AppInstance appInstance = appInstanceRepository.findFirstById(id);
    //    param.patchAppInstance(appInstance, getUserEmployeeId());
    //    appInstanceRepository.saveAndFlush(appInstance);
    //    appUpdateService.replaceResourceQuota(appInstance);
    //    return buildSucceedResult("OK");
    //}

    @ApiOperation(value = "update")
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public TeslaBaseResult update(Long id, @RequestBody AppInstanceUpdateParam param) {
        AppInstance appInstance = appInstanceRepository.findFirstById(id);
        param.patchAppInstance(appInstance, getUserEmployeeId());
        appInstanceRepository.saveAndFlush(appInstance);
        return buildSucceedResult("ok");
    }

    @ApiOperation(value = "getClusterResources")
    @RequestMapping(value = "getClusterResources", method = RequestMethod.GET)
    public TeslaBaseResult getClusterResources(Long id) {
        AppInstance appInstance = appInstanceRepository.findFirstById(id);
        return buildSucceedResult(appInstance.detail().clusterResourceIdList().stream()
            .map(clusterResourceId -> {
                JSONObject ret = clusterResourceRepository.findObjectById(clusterResourceId);
                RegularUtil.underscoreToCamel(ret);
                RegularUtil.gmt2Date(ret);
                return ret;
            })
            .collect(Collectors.toList()));
    }

}
