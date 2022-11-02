package com.alibaba.sreworks.plugin.server.services;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.sreworks.common.util.UuidUtil;
import com.alibaba.sreworks.plugin.server.DTO.Account;
import com.alibaba.sreworks.plugin.server.DTO.Instance;
import com.alibaba.sreworks.plugin.server.DTO.InstanceDetail;
import com.alibaba.sreworks.plugin.server.DTO.UsageDetail;

import com.aliyun.rds20140815.models.CreateAccountRequest;
import com.aliyun.rds20140815.models.DescribeDBInstanceNetInfoRequest;
import com.aliyun.rds20140815.models.DescribeDBInstanceNetInfoResponse;
import com.aliyun.rds20140815.models.DescribeDBInstancesRequest;
import com.aliyun.rds20140815.models.DescribeDBInstancesResponse;
import com.aliyun.rds20140815.models.DescribeRegionsRequest;
import com.aliyun.rds20140815.models.DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRDSRegion;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class ResourceService {

    public com.aliyun.rds20140815.Client createClient(Account account) throws Exception {
        Config config = new Config()
            .setAccessKeyId(account.getAccessKeyId())
            .setAccessKeySecret(account.getAccessKeySecret());
        config.endpoint = "rds.aliyuncs.com";
        return new com.aliyun.rds20140815.Client(config);
    }

    private List<String> regions(Account account) throws Exception {
        com.aliyun.rds20140815.Client client = createClient(account);
        DescribeRegionsRequest describeRegionsRequest = new DescribeRegionsRequest();
        return client.describeRegions(describeRegionsRequest).getBody().getRegions().getRDSRegion()
            .stream().map(DescribeRegionsResponseBodyRegionsRDSRegion::getRegionId).collect(Collectors.toList());
    }

    public List<Instance> list(Account account, String engine) throws Exception {
        com.aliyun.rds20140815.Client client = createClient(account);
        DescribeDBInstancesRequest request = new DescribeDBInstancesRequest().setRegionId("cn");
        DescribeDBInstancesResponse response = client.describeDBInstances(request);
        return response.getBody().getItems().getDBInstance().stream()
            .map(dbInstance -> Instance.builder()
                .name(dbInstance.getDBInstanceId())
                .alias(String.format("%s-%s-%s",
                    dbInstance.getEngine(), dbInstance.getEngineVersion(), dbInstance.getDBInstanceId())
                )
                .status(dbInstance.getDBInstanceStatus())
                .isNormal("Running".equals(dbInstance.getDBInstanceStatus()))
                .detail(InstanceDetail.builder()
                    .engine(dbInstance.getEngine())
                    .engineVersion(dbInstance.getEngineVersion())
                    .regionId(dbInstance.getRegionId())
                    .instanceId(dbInstance.getDBInstanceId())
                    .build())
                .build()
            )
            .filter(Objects::nonNull)
            .filter(x -> engine == null || x.getDetail().getEngine().equals(engine))
            .collect(Collectors.toList());
    }

    private String createAccount(Account account, String name) throws Exception {
        String uuid = "sw5_" + UuidUtil.shortUuid();
        com.aliyun.rds20140815.Client client = createClient(account);
        CreateAccountRequest createAccountRequest = new CreateAccountRequest()
            .setDBInstanceId(name)
            .setAccountName(uuid)
            .setAccountPassword(uuid);
        client.createAccount(createAccountRequest);
        return uuid;
    }

    public UsageDetail getUsageDetail(Account account, String name) throws Exception {
        com.aliyun.rds20140815.Client client = createClient(account);
        DescribeDBInstanceNetInfoRequest request = new DescribeDBInstanceNetInfoRequest().setDBInstanceId(name);
        DescribeDBInstanceNetInfoResponse response = client.describeDBInstanceNetInfo(request);
        String accountNamePassword = createAccount(account, name);
        UsageDetail usageDetail = UsageDetail.builder()
            .user(accountNamePassword)
            .password(accountNamePassword)
            .build();
        response.getBody().getDBInstanceNetInfos().getDBInstanceNetInfo().forEach(x -> {
            switch (x.getIPType()) {
                case "Inner":
                    usageDetail.setInnerHost(x.getConnectionString());
                    usageDetail.setInnerPort(x.getPort());
                case "Public":
                    usageDetail.setPublicHost(x.getConnectionString());
                    usageDetail.setPublicPort(x.getPort());
                default:
                    break;
            }
        });
        return usageDetail;
    }

}
