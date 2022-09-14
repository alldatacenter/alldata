package com.alibaba.sreworks.clustermanage.server.services;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.DTO.RunCmdOutPut;
import com.alibaba.sreworks.common.util.CmdUtil;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.domain.DO.Cluster;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class DeployOperatorService {

    public V1StatefulSet getOperatorSts(Cluster cluster) throws IOException, ApiException {
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        AppsV1Api appsV1Api = new AppsV1Api(client);
        List<V1StatefulSet> statefulSetList = appsV1Api.listNamespacedStatefulSet("default", null, null, null,
            "metadata.name=abm-operator", null, null, null, null, null, null).getItems();
        return CollectionUtils.isEmpty(statefulSetList) ? null : statefulSetList.get(0);
    }

    public boolean checkOperatorStsStatus(Cluster cluster) throws IOException, ApiException {
        V1StatefulSet sts = getOperatorSts(cluster);
        if (sts.getStatus() == null || sts.getStatus().getReadyReplicas() == null
            || sts.getStatus().getReplicas() == null) {
            return false;
        }
        return Objects.equals(sts.getStatus().getReadyReplicas(), sts.getStatus().getReplicas());
    }

    public float progressOperator(Cluster cluster) throws IOException, ApiException {
        V1StatefulSet sts = getOperatorSts(cluster);
        if (sts.getStatus() == null || sts.getStatus().getReadyReplicas() == null
            || sts.getStatus().getReplicas() == null) {
            return 0;
        }
        return 0.1f * sts.getStatus().getReadyReplicas() / sts.getStatus().getReplicas();
    }

    public RunCmdOutPut checkOperatorDeployed(Cluster cluster) throws IOException, InterruptedException {
        String filePath = K8sUtil.getKubeConfigFile(cluster.getKubeconfig()).getAbsolutePath();
        String cmd = "helm status sw-operator -o json --kubeconfig=" + filePath;
        return CmdUtil.exec(cmd, 10);
    }

    public boolean isOperatorDeployed(RunCmdOutPut runCmdOutPut) {
        if (runCmdOutPut.getCode() == 0) {
            String status = JSONObject.parseObject(runCmdOutPut.getStdout()).getJSONObject("info").getString("status");
            return "deployed".equals(status);
        }
        return false;
    }

    public void deployOperator(Cluster cluster) throws Exception {
        String filePath = K8sUtil.getKubeConfigFile(cluster.getKubeconfig()).getAbsolutePath();
        String cmd = "helm install sw-operator /tmp/sw-operator-chart "
            + "-f /tmp/sw-operator-chart-values.yaml --kubeconfig=" + filePath;
        CmdUtil.exec(cmd, 10);
    }

    public void deployAndCheckOperator(Cluster cluster) throws Exception {
        RunCmdOutPut runCmdOutPut;
        runCmdOutPut = checkOperatorDeployed(cluster);
        if (runCmdOutPut.getCode() != 0) {
            deployOperator(cluster);
            runCmdOutPut = checkOperatorDeployed(cluster);
        }
        if (!isOperatorDeployed(runCmdOutPut)) {
            throw new Exception(runCmdOutPut.toString());
        }
    }

}
