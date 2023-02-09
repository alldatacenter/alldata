package com.qcloud.cos;

import com.qcloud.cos.model.*;
import com.qcloud.cos.model.inventory.*;
import com.qcloud.cos.model.inventory.InventoryFrequency;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BucketInventoryTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setGetDelBucketInventoryTest() {
        if(accountId == null) {
            return;
        }
        InventoryConfiguration inventoryConfiguration = new InventoryConfiguration();
        InventoryCosBucketDestination inventoryCosBucketDestination = new InventoryCosBucketDestination();
        // 设置清单的输出目标存储桶的格式和前缀等
        inventoryCosBucketDestination.setAccountId(accountId);
        String bucketArn = "qcs::cos:" + region + "::" + bucket;
        inventoryCosBucketDestination.setBucketArn(bucketArn);
        inventoryCosBucketDestination.setEncryption(new ServerSideEncryptionCOS());
        inventoryCosBucketDestination.setFormat(InventoryFormat.CSV);
        inventoryCosBucketDestination.setPrefix("inventory-output");
        InventoryDestination inventoryDestination = new InventoryDestination();
        inventoryDestination.setCosBucketDestination(inventoryCosBucketDestination);
        inventoryConfiguration.setDestination(inventoryDestination);

        // 设置清单的调度周期，扫描前缀和id等
        inventoryConfiguration.setEnabled(true);
        inventoryConfiguration.setId("1");
        InventorySchedule inventorySchedule = new InventorySchedule();
        inventorySchedule.setFrequency(InventoryFrequency.Daily);
        inventoryConfiguration.setSchedule(inventorySchedule);
        InventoryPrefixPredicate inventoryFilter = new InventoryPrefixPredicate("test/");
        inventoryConfiguration.setInventoryFilter(new InventoryFilter(inventoryFilter));
        inventoryConfiguration.setIncludedObjectVersions(InventoryIncludedObjectVersions.All);
        // 设置可选的输出字段
        List<String> optionalFields = new LinkedList<String>();
        optionalFields.add(InventoryOptionalField.Size.toString());
        inventoryConfiguration.setOptionalFields(optionalFields);
        SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest = new SetBucketInventoryConfigurationRequest();
        setBucketInventoryConfigurationRequest.setBucketName(bucket);
        setBucketInventoryConfigurationRequest.setInventoryConfiguration(inventoryConfiguration);
        cosclient.setBucketInventoryConfiguration(setBucketInventoryConfigurationRequest);

        inventoryConfiguration.setId("2");
        inventorySchedule.setFrequency(InventoryFrequency.Weekly);
        cosclient.setBucketInventoryConfiguration(setBucketInventoryConfigurationRequest);

        // 获取指定id的清单配置
        GetBucketInventoryConfigurationResult getBucketInventoryConfigurationResult = cosclient.getBucketInventoryConfiguration(bucket, "2");
        InventoryConfiguration inventoryConfiguration1 = getBucketInventoryConfigurationResult.getInventoryConfiguration();
        InventoryCosBucketDestination inventoryCosBucketDestination1 = inventoryConfiguration1.getDestination().getCosBucketDestination();
        assertEquals(inventoryConfiguration.getId(), inventoryConfiguration1.getId());
        assertEquals(inventoryConfiguration.getIncludedObjectVersions(), inventoryConfiguration1.getIncludedObjectVersions());
        assertEquals(inventoryConfiguration.getSchedule().getFrequency(), inventoryConfiguration1.getSchedule().getFrequency());
        assertEquals(inventoryConfiguration.getOptionalFields(), inventoryConfiguration1.getOptionalFields());

        assertEquals(inventoryCosBucketDestination.getAccountId(), inventoryCosBucketDestination1.getAccountId());
        assertEquals(inventoryCosBucketDestination.getBucketArn(), inventoryCosBucketDestination1.getBucketArn());
        assertEquals(inventoryCosBucketDestination.getFormat(), inventoryCosBucketDestination1.getFormat());
        assertEquals(inventoryCosBucketDestination.getPrefix(), inventoryCosBucketDestination1.getPrefix());

        // 批量获取清单
        ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest = new ListBucketInventoryConfigurationsRequest();
        listBucketInventoryConfigurationsRequest.setBucketName(bucket);
        ListBucketInventoryConfigurationsResult listBucketInventoryConfigurationsResult = cosclient.listBucketInventoryConfigurations(listBucketInventoryConfigurationsRequest);
        assertEquals(2, listBucketInventoryConfigurationsResult.getInventoryConfigurationList().size());

        // 删除指定清单
        DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest = new DeleteBucketInventoryConfigurationRequest();
        deleteBucketInventoryConfigurationRequest.setBucketName(bucket);
        deleteBucketInventoryConfigurationRequest.setId("1");
        cosclient.deleteBucketInventoryConfiguration(deleteBucketInventoryConfigurationRequest);

        // 获取剩余清单
        ListBucketInventoryConfigurationsResult listBucketInventoryConfigurationsResult1 = cosclient.listBucketInventoryConfigurations(listBucketInventoryConfigurationsRequest);
        assertEquals(1, listBucketInventoryConfigurationsResult1.getInventoryConfigurationList().size());
    }
}
