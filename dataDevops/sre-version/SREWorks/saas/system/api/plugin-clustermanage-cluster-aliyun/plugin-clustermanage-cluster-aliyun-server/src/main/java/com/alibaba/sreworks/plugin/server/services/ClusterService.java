package com.alibaba.sreworks.plugin.server.services;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.plugin.server.DTO.*;

import com.aliyun.cs20151215.Client;
import com.aliyun.cs20151215.models.DescribeClusterAddonsVersionResponse;
import com.aliyun.cs20151215.models.DescribeClusterDetailResponse;
import com.aliyun.cs20151215.models.DescribeClusterDetailResponseBody;
import com.aliyun.cs20151215.models.DescribeClusterUserKubeconfigRequest;
import com.aliyun.cs20151215.models.DescribeClusterUserKubeconfigResponse;
import com.aliyun.cs20151215.models.DescribeClustersV1Request;
import com.aliyun.cs20151215.models.DescribeClustersV1Response;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class ClusterService {

    private Client client(Account account) throws Exception {
        Config config = new Config()
            .setAccessKeyId(account.getAccessKeyId())
            .setAccessKeySecret(account.getAccessKeySecret());
        config.endpoint = "cs.cn-beijing.aliyuncs.com";
        return new Client(config);
    }

    public List<Cluster> list(Account account) throws Exception {
        DescribeClustersV1Request request = new DescribeClustersV1Request();
        DescribeClustersV1Response response = client(account).describeClustersV1(request);
        return response.getBody().getClusters().stream()
            .map(cluster -> Cluster.builder()
                .name(cluster.getClusterId())
                .alias(cluster.getName())
                .status(cluster.getState())
                .isNormal("running".equals(cluster.getState()))
                .detail(
                    ClusterDetail.builder()
                        .clusterType(cluster.getClusterType())
                        .regionId(cluster.getRegionId())
                        .build()
                )
                .build())
            .collect(Collectors.toList());
    }

    public Cluster get(Account account, String name) throws Exception {
        DescribeClusterDetailResponse response = client(account).describeClusterDetail(name);
        DescribeClusterDetailResponseBody cluster = response.getBody();
        return Cluster.builder()
            .name(cluster.getClusterId())
            .alias(cluster.getName())
            .status(cluster.getState())
            .isNormal("running".equals(cluster.getState()))
            .detail(
                ClusterDetail.builder()
                    .clusterType(cluster.getClusterType())
                    .regionId(cluster.getRegionId())
                    .build()
            )
            .build();
    }

    public String getKubeConfig(Account account, String name) throws Exception {
        DescribeClusterUserKubeconfigRequest request = new DescribeClusterUserKubeconfigRequest();
        DescribeClusterUserKubeconfigResponse response = client(account)
            .describeClusterUserKubeconfig(name, request);
        return response.getBody().getConfig();
    }

    public String getIngressHost(Account account, String name, String subIngressHost) throws Exception {
        DescribeClusterDetailResponse response = client(account).describeClusterDetail(name);
        DescribeClusterDetailResponseBody cluster = response.getBody();
        return String.format(
            "%s.%s.%s.alicontainer.com",
            subIngressHost, cluster.getClusterId(), cluster.getRegionId()
        );
    }

    public List<Component> getComponent(Account account, String name) throws Exception {
        DescribeClusterAddonsVersionResponse response = client(account).describeClusterAddonsVersion(name);
        return response.getBody().values().stream().map(c -> {
            JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(c));
            return Component.builder()
                .category(jsonObject.getString("category"))
                .name(jsonObject.getString("component_name"))
                .version(jsonObject.getString("version"))
                .nextVersion(jsonObject.getString("next_version"))
                .canUpgrade(jsonObject.getBooleanValue("can_upgrade"))
                .isDeployed(!StringUtil.isEmpty(jsonObject.getString("version")))
                .build();
        }).collect(Collectors.toList());
    }

}
