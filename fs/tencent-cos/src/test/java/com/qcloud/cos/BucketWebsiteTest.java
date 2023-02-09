package com.qcloud.cos;

import com.qcloud.cos.model.BucketWebsiteConfiguration;
import com.qcloud.cos.model.RedirectRule;
import com.qcloud.cos.model.RoutingRule;
import com.qcloud.cos.model.RoutingRuleCondition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BucketWebsiteTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void testSetGetDeleteBucketWebsite() {
        // 设置bucket website
        BucketWebsiteConfiguration bucketWebsiteConfiguration = new BucketWebsiteConfiguration();
        bucketWebsiteConfiguration.setIndexDocumentSuffix("index.html");
        bucketWebsiteConfiguration.setErrorDocument("error.html");
        List<RoutingRule> routingRuleList = new ArrayList<RoutingRule>();
        RoutingRule routingRule = new RoutingRule();
        RoutingRuleCondition routingRuleCondition = new RoutingRuleCondition();
        routingRuleCondition.setHttpErrorCodeReturnedEquals("404");
        routingRule.setCondition(routingRuleCondition);
        RedirectRule redirectRule = new RedirectRule();
        redirectRule.setProtocol("https");
        redirectRule.setReplaceKeyPrefixWith("404.html");
        routingRule.setRedirect(redirectRule);
        routingRuleList.add(routingRule);
        bucketWebsiteConfiguration.setRoutingRules(routingRuleList);
        cosclient.setBucketWebsiteConfiguration(bucket, bucketWebsiteConfiguration);

        // 获取bucket website
        BucketWebsiteConfiguration bucketWebsiteConfiguration1 = cosclient.getBucketWebsiteConfiguration(bucket);
        assertEquals(bucketWebsiteConfiguration.getIndexDocumentSuffix(), bucketWebsiteConfiguration1.getIndexDocumentSuffix());
        assertEquals(bucketWebsiteConfiguration.getErrorDocument(), bucketWebsiteConfiguration1.getErrorDocument());
        assertTrue(1 == bucketWebsiteConfiguration1.getRoutingRules().size());

        RoutingRule routingRule1 = bucketWebsiteConfiguration1.getRoutingRules().get(0);
        assertEquals(routingRule.getCondition().getHttpErrorCodeReturnedEquals(),
                routingRule1.getCondition().getHttpErrorCodeReturnedEquals());
        assertEquals(routingRule.getRedirect().getprotocol(), routingRule1.getRedirect().getprotocol());
        assertEquals(routingRule.getRedirect().getReplaceKeyPrefixWith(), routingRule1.getRedirect().getReplaceKeyPrefixWith());

        // 删除bucket website
        cosclient.deleteBucketWebsiteConfiguration(bucket);
    }

}
