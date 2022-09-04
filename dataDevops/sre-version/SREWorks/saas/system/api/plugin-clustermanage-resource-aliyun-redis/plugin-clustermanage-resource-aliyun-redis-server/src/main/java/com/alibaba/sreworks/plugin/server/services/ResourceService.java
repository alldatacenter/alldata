package com.alibaba.sreworks.plugin.server.services;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.sreworks.common.util.UuidUtil;
import com.alibaba.sreworks.plugin.server.DTO.Account;
import com.alibaba.sreworks.plugin.server.DTO.Instance;
import com.alibaba.sreworks.plugin.server.DTO.InstanceDetail;
import com.alibaba.sreworks.plugin.server.DTO.UsageDetail;

import com.aliyun.r_kvstore20150101.Client;
import com.aliyun.r_kvstore20150101.models.CreateAccountRequest;
import com.aliyun.r_kvstore20150101.models.DescribeDBInstanceNetInfoRequest;
import com.aliyun.r_kvstore20150101.models.DescribeDBInstanceNetInfoResponse;
import com.aliyun.r_kvstore20150101.models.DescribeInstancesRequest;
import com.aliyun.r_kvstore20150101.models.DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesKVStoreInstance;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class ResourceService {

    private Client client(Account account) throws Exception {
        Config config = new Config()
            .setAccessKeyId(account.getAccessKeyId())
            .setAccessKeySecret(account.getAccessKeySecret());
        config.endpoint = "r-kvstore.aliyuncs.com";
        return new Client(config);
    }

    private Instance instance(DescribeInstancesResponseBodyInstancesKVStoreInstance kvStoreInstance) {
        return Instance.builder()
            .name(kvStoreInstance.getInstanceId())
            .alias(kvStoreInstance.getInstanceName())
            .status(kvStoreInstance.getInstanceStatus())
            .isNormal("Normal".equals(kvStoreInstance.getInstanceStatus()))
            .detail(
                InstanceDetail.builder()
                    .architectureType(kvStoreInstance.getArchitectureType())
                    .regionId(kvStoreInstance.getRegionId())
                    .instanceId(kvStoreInstance.getInstanceId())
                    .connectionDomain(kvStoreInstance.getConnectionDomain())
                    .port(kvStoreInstance.getPort())
                    .build()
            )
            .build();
    }

    public List<Instance> list(Account account) throws Exception {
        Client client = client(account);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
            .setTag(Collections.singletonList(new DescribeInstancesRequest.DescribeInstancesRequestTag()));
        return client.describeInstances(describeInstancesRequest).getBody().getInstances().getKVStoreInstance().stream()
            .map(this::instance).collect(Collectors.toList());
    }

    public Instance get(Account account, String name) throws Exception {
        Client client = client(account);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
            .setTag(Collections.singletonList(new DescribeInstancesRequest.DescribeInstancesRequestTag()));
        describeInstancesRequest.setInstanceIds(name);
        return instance(
            client.describeInstances(describeInstancesRequest).getBody().getInstances().getKVStoreInstance().get(0)
        );
    }

    public UsageDetail getUsageDetail(Account account, String name) throws Exception {
        String uuid = "sw5_" + UuidUtil.shortUuid();
        Client client = client(account);
        Instance instance = get(account, name);

        CreateAccountRequest createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setInstanceId(instance.getDetail().getInstanceId());
        createAccountRequest.setAccountName(uuid);
        createAccountRequest.setAccountPassword(uuid);
        client.createAccount(createAccountRequest);

        DescribeDBInstanceNetInfoRequest netInfoRequest = new DescribeDBInstanceNetInfoRequest();
        netInfoRequest.setInstanceId(instance.getDetail().getInstanceId());
        DescribeDBInstanceNetInfoResponse response = client.describeDBInstanceNetInfo(netInfoRequest);
        UsageDetail usageDetail = UsageDetail.builder().user(uuid).password(uuid).build();

        response.getBody().getNetInfoItems().getInstanceNetInfo().forEach(x -> {
            switch (x.getIPType()) {
                case "Public":
                    usageDetail.setPublicHost(x.getConnectionString());
                    usageDetail.setPublicPort(x.getPort());
                    break;
                case "Private":
                    usageDetail.setPrivateHost(x.getConnectionString());
                    usageDetail.setPrivatePort(x.getPort());
                    break;
                case "Inner":
                    usageDetail.setInnerHost(x.getConnectionString());
                    usageDetail.setInnerPort(x.getPort());
                    break;
                default:
                    break;
            }
        });
        return usageDetail;
    }

}
