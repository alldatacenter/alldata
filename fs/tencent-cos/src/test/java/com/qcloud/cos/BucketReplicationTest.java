package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.qcloud.cos.model.BucketReplicationConfiguration;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.ReplicationDestinationConfig;
import com.qcloud.cos.model.ReplicationRule;
import com.qcloud.cos.model.ReplicationRuleStatus;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.StorageClass;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BucketReplicationTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setGetDelBucketReplicationTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        ReplicationRule replicationRule = new ReplicationRule();
        String prefix = "copy1_folder/";
        replicationRule.setPrefix(prefix);
        replicationRule.setStatus(ReplicationRuleStatus.Enabled);

        ReplicationDestinationConfig replicationDestinationConfig =
                new ReplicationDestinationConfig();
        String bucketQCS = "qcs:id/0:cos:ap-chengdu:appid/1251668577:chengwus3cd";
        replicationDestinationConfig.setBucketQCS(bucketQCS);
        replicationDestinationConfig.setStorageClass(StorageClass.Standard);
        replicationRule.setDestinationConfig(replicationDestinationConfig);
        BucketReplicationConfiguration bucketReplicationConfiguration =
                new BucketReplicationConfiguration();
        String ruleName = "qcs::cam::uin/123456789:uin/987654543";
        bucketReplicationConfiguration.setRoleName(ruleName);
        String ruleId = "cctest1";
        replicationRule.setID(ruleId);
        bucketReplicationConfiguration.addRule(replicationRule);
        cosclient.setBucketReplicationConfiguration(bucket, bucketReplicationConfiguration);

        // replication设置后, 立刻获取会需要一段时间
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        BucketReplicationConfiguration replicaConfigGet =
                cosclient.getBucketReplicationConfiguration(bucket);
        assertEquals(ruleName, replicaConfigGet.getRoleName());
        assertEquals(1, replicaConfigGet.getRules().size());
        ReplicationRule replicationRule2 = replicaConfigGet.getRules().get(0);
        assertEquals(bucketQCS, replicationRule2.getDestinationConfig().getBucketQCS());
        assertEquals(StorageClass.Standard.toString(),
                replicationRule2.getDestinationConfig().getStorageClass());
        assertEquals(ReplicationRuleStatus.Enabled.toString(), replicationRule2.getStatus());
        assertEquals(prefix, replicationRule2.getPrefix());

        cosclient.deleteBucketReplicationConfiguration(bucket);
    }


}
