package com.alibaba.sreworks.clustermanage.server.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.clustermanage.server.params.ClusterCreateParam;
import com.alibaba.sreworks.clustermanage.server.params.ClusterDeployClientParam;
import com.alibaba.sreworks.clustermanage.server.params.ClusterModifyParam;
import com.alibaba.sreworks.clustermanage.server.services.ClientPackageService;
import com.alibaba.sreworks.clustermanage.server.services.DeployClientService;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerClusterService;
import com.alibaba.sreworks.flyadmin.server.services.PluginClusterService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import com.google.gson.JsonArray;
import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/cluster")
@Api(tags = "集群")
public class ClusterController extends BaseController {

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    PluginClusterService pluginClusterService;

    @Autowired
    FlyadminAppmanagerClusterService flyadminAppmanagerClusterService;

    @Autowired
    DeployClientService deployClientService;

    @Autowired
    private ClientPackageService clientPackageService;

    @ApiOperation(value = "创建")
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(@RequestBody ClusterCreateParam param)
        throws IOException, ApiException, InterruptedException {
        Cluster cluster = param.toCluster(getUserEmployeeId());
        String kubeConfig;
        if(StringUtils.isEmpty(param.getKubeconfig())){
            kubeConfig = pluginClusterService.getKubeConfig(
                    cluster.getAccountId(), param.getClusterName(), getUserEmployeeId());
        }else{
            kubeConfig = param.getKubeconfig();
        }
        if(!StringUtils.isEmpty(param.getDeployClient()) && param.getDeployClient().equals("enable")){
            deployClientService.run(kubeConfig);
        }
        cluster.setKubeconfig(kubeConfig);
        clusterRepository.saveAndFlush(cluster);
        flyadminAppmanagerClusterService.create(cluster, getUserEmployeeId());
        JSONObject result = new JSONObject();
        result.put("clusterId", cluster.getId());
        result.put("teamId", cluster.getTeamId());
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "删除")
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(id);
        clusterRepository.deleteById(id);
        flyadminAppmanagerClusterService.delete(id, getUserEmployeeId());
        JSONObject result = new JSONObject();
        result.put("clusterId", id);
        result.put("result", "OK");
        result.put("teamId", cluster.getTeamId());
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "修改")
    @RequestMapping(value = "modify", method = RequestMethod.PUT)
    public TeslaBaseResult modify(Long id, @RequestBody ClusterModifyParam param) {
        Cluster cluster = clusterRepository.findFirstById(id);
        param.patchCluster(cluster, getUserEmployeeId());
        clusterRepository.saveAndFlush(cluster);
        JSONObject result = new JSONObject();
        result.put("clusterId", cluster.getId());
        result.put("result", "OK");
        result.put("teamId", cluster.getTeamId());
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "详情")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        JSONObject ret = clusterRepository.findObjectById(id);
        RegularUtil.gmt2Date(ret);
        RegularUtil.underscoreToCamel(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "列表")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(String name) {
        name = StringUtil.isEmpty(name) ? "" : name;
        List<JSONObject> ret = clusterRepository.findObjectByUserAndNameLike(getUserEmployeeId(), "%" + name + "%");
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        for(JSONObject clusterInfo: ret){
            clusterInfo.put("kubeconfig", null);
        }
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "全部列表")
    @RequestMapping(value = "listAll", method = RequestMethod.GET)
    public TeslaBaseResult listAll() {
        List<JSONObject> ret = clusterRepository.findAll().stream().map(Cluster::toJsonObject).collect(Collectors.toList());
        for(JSONObject clusterInfo: ret){
            clusterInfo.put("kubeconfig", null);
        }
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

//    @ApiOperation(value = "公共列表")
//    @RequestMapping(value = "listPublic", method = RequestMethod.GET)
//    public TeslaBaseResult listPublic(String name) {
//        name = StringUtil.isEmpty(name) ? "" : name;
//        List<JSONObject> ret = clusterRepository.findObjectByVisibleScopeIsPublicAndNameLike("%" + name + "%");
//        RegularUtil.underscoreToCamel(ret);
//        RegularUtil.gmt2Date(ret);
//        return buildSucceedResult(ret);
//    }

    @ApiOperation(value = "idSelector")
    @RequestMapping(value = "idSelector", method = RequestMethod.GET)
    public TeslaBaseResult idSelector(Long teamId) {
        List<Cluster> clusterList = clusterRepository.findAllByTeamId(teamId);
        return buildSucceedResult(JsonUtil.map(
            "options", clusterList.stream().map(cluster -> JsonUtil.map(
                "label", cluster.getName(),
                "value", cluster.getId()
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "纳管状态查询")
    @RequestMapping(value = "checkClientStatus", method = RequestMethod.GET)
    public TeslaBaseResult checkClientStatus(Long id) throws IOException, InterruptedException {
        JSONObject ret = clusterRepository.findObjectById(id);
        JSONObject resultDict = new JSONObject();
        for(String clientName: clientPackageService.getNames()){
            JSONObject obj = new JSONObject();
            JSONObject clientInfo = clientPackageService.getValue(clientName);
            obj.put("installStatus", false);
            if(StringUtils.isNotBlank(clientInfo.getString("chart"))){
                obj.put("chart", clientInfo.getString("chart"));
            }
            if(StringUtils.isNotBlank(clientInfo.getString("name"))){
                obj.put("name", clientInfo.getString("name"));
            }
            if(StringUtils.isNotBlank(clientInfo.getString("chart"))) {
                resultDict.put(clientInfo.getString("chart"), obj);
            }
        }


        JSONArray helmList = deployClientService.listHelmInstallStatus(ret.getString("kubeconfig"));

        for (int i = 0; i < helmList.size(); i++) {
            JSONObject helmObject = helmList.getJSONObject(i);
            String chartNameVersion = helmObject.getString("chart");
            if(StringUtils.isNotBlank(chartNameVersion)){
                if(!chartNameVersion.contains("-")){
                    continue;
                }
                String[] items = chartNameVersion.split("-");
                String version = items[items.length -1];
                String name = Arrays.stream(Arrays.copyOfRange(items, 0, items.length - 1))
                        .collect(Collectors.joining("-"));
                if(resultDict.getJSONObject(name) != null){
                    JSONObject targetObject = resultDict.getJSONObject(name);
                    targetObject.put("currentVersion", version);
                    if(StringUtils.isNotBlank(helmObject.getString("namespace"))){
                        targetObject.put("currentNamespace", helmObject.getString("namespace"));
                    }
                    if(StringUtils.isNotBlank(helmObject.getString("status"))){
                        targetObject.put("currentHelmStatus", helmObject.getString("status"));
                        if(helmObject.getString("status").equals("deployed")){
                            targetObject.put("installStatus", true);
                        }
                    }
                }
            }

        }

        return buildSucceedResult(resultDict.values());
    }


    @ApiOperation(value = "纳管客户端重新部署")
    @RequestMapping(value = "redeployClient", method = RequestMethod.PUT)
    public TeslaBaseResult redeployClient(Long id,
              @RequestBody ClusterDeployClientParam param) throws IOException, InterruptedException, TimeoutException {
        JSONObject ret = clusterRepository.findObjectById(id);
        JSONObject result = deployClientService.installAllPackages(ret.getString("kubeconfig"), param.getEnvMap());
        return buildSucceedResult(result.values());
    }
}
