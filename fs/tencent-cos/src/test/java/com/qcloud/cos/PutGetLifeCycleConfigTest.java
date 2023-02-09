package com.qcloud.cos;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.SetBucketLifecycleConfigurationRequest;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.qcloud.cos.model.BucketLifecycleConfiguration.NoncurrentVersionTransition;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Rule;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Transition;
import com.qcloud.cos.model.BucketVersioningConfiguration;

public class PutGetLifeCycleConfigTest extends AbstractCOSClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    

    private void testPutGetDelRules(List<Rule> rules) {
        BucketLifecycleConfiguration bucketLifecycleConfiguration =
                new BucketLifecycleConfiguration();
        bucketLifecycleConfiguration.setRules(rules);
        SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest =
                new SetBucketLifecycleConfigurationRequest(bucket, bucketLifecycleConfiguration);
        cosclient.setBucketLifecycleConfiguration(setBucketLifecycleConfigurationRequest);

        try {
            Thread.sleep(4000L); // put 后立刻get会存在获取不到的可能
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BucketLifecycleConfiguration queryLifeCycleRet =
                cosclient.getBucketLifecycleConfiguration(bucket);
        List<Rule> ruleLists = queryLifeCycleRet.getRules();
        assertEquals(rules.size(), ruleLists.size());
        for (int ruleIndex = 0; ruleIndex < rules.size(); ++ruleIndex) {
            Rule ruleSet = rules.get(ruleIndex);
            Rule ruleQuery = ruleLists.get(ruleIndex);
            assertEquals(ruleSet.getId(), ruleQuery.getId());
            assertEquals(ruleSet.getExpirationInDays(), ruleQuery.getExpirationInDays());
            if (ruleSet.getFilter() != null) {
                assertTrue(ruleQuery.getFilter() != null);
                LifecyclePrefixPredicate prefixSet =
                        (LifecyclePrefixPredicate) ruleSet.getFilter().getPredicate();
                LifecyclePrefixPredicate prefixGet =
                        (LifecyclePrefixPredicate) ruleSet.getFilter().getPredicate();
                assertEquals(prefixSet.getPrefix(), prefixGet.getPrefix());
            }
        }

        cosclient.deleteBucketLifecycleConfiguration(bucket);
    }

    @Test
    public void testGetLifecycleBeforeSet() {
        if (!judgeUserInfoValid()) {
            return;
        }
        
        BucketLifecycleConfiguration blcf = cosclient.getBucketLifecycleConfiguration(bucket);
        assertNull(blcf);
    }
    
    @Test
    public void testPutGetDelExpirationDateLifeCycle() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        List<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        rule.setId("delete prefix xxxy at a date");
        rule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate("test/")));
        Calendar c1 = Calendar.getInstance();
        c1.set(2017, 11, 01, 8, 00, 00);
        rule.setExpirationDate(c1.getTime());
        rule.setStatus(BucketLifecycleConfiguration.ENABLED);
        rules.add(rule);
        testPutGetDelRules(rules);
    }

    @Test
    public void testPutGetDelLifeCycleForNormalBucket() {
        if (!judgeUserInfoValid()) {
            return;
        }
        List<Rule> rules = new ArrayList<>();

        // 30天后删除
        Rule deletePrefixRule = new Rule();
        deletePrefixRule.setId("delete prefix xxxy after 30 days");
        deletePrefixRule
                .setFilter(new LifecycleFilter(new LifecyclePrefixPredicate("delete_prefix/")));
        deletePrefixRule.setExpirationInDays(30);
        deletePrefixRule.setStatus(BucketLifecycleConfiguration.ENABLED);

        // 20天后沉降到低频，一年后删除
        Rule standardIaRule = new Rule();
        standardIaRule.setId("standard_ia transition");
        standardIaRule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate("standard_ia/")));
        List<Transition> standardIaTransitions = new ArrayList<>();
        Transition standardTransition = new Transition();
        standardTransition.setDays(20);
        standardTransition.setStorageClass(StorageClass.Standard_IA.toString());
        standardIaTransitions.add(standardTransition);
        standardIaRule.setTransitions(standardIaTransitions);
        standardIaRule.setStatus(BucketLifecycleConfiguration.DISABLED);
        standardIaRule.setExpirationInDays(120);

        rules.add(deletePrefixRule);
        rules.add(standardIaRule);
        testPutGetDelRules(rules);
    }

    @Ignore
    public void testPutGetDelLifeCycleForBucketWithVersions() {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersionConfig =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        SetBucketVersioningConfigurationRequest setBucketVersionReq =
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersionConfig);
        cosclient.setBucketVersioningConfiguration(setBucketVersionReq);

        List<Rule> rules = new ArrayList<>();

        Rule standardIaRule = new Rule();
        standardIaRule.setId("standard_ia transition for version");
        standardIaRule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate("standard_ia/")));
        List<Transition> standardIaTransitions = new ArrayList<>();
        Transition standardTransition = new Transition();
        standardTransition.setDays(20);
        standardTransition.setStorageClass(StorageClass.Standard_IA.toString());
        standardIaTransitions.add(standardTransition);
        standardIaRule.setTransitions(standardIaTransitions);
        standardIaRule.setStatus(BucketLifecycleConfiguration.DISABLED);
        standardIaRule.setNoncurrentVersionExpirationInDays(360);
        List<NoncurrentVersionTransition> noncurrentVersionTransitions = new ArrayList<>();
        NoncurrentVersionTransition noncurrentVersionTransition = new NoncurrentVersionTransition();
        noncurrentVersionTransition.setDays(30);
        noncurrentVersionTransition.setStorageClass(StorageClass.Standard_IA);
        standardIaRule.setNoncurrentVersionTransitions(noncurrentVersionTransitions);
        standardIaRule.setExpirationInDays(120);
        
        rules.add(standardIaRule);
        testPutGetDelRules(rules);

        bucketVersionConfig.setStatus(BucketVersioningConfiguration.SUSPENDED);
        SetBucketVersioningConfigurationRequest closeBucketVersionReq =
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersionConfig);
        cosclient.setBucketVersioningConfiguration(closeBucketVersionReq);
    }

}
