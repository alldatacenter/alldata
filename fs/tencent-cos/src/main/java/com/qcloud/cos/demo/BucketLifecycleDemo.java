package com.qcloud.cos.demo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.SetBucketLifecycleConfigurationRequest;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.qcloud.cos.region.Region;

	
public class BucketLifecycleDemo {
    public  String bucketName = "example-1251668577";
    public COSClient cosClient = COSBuilder();

    public COSClient COSBuilder() {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-shanghai"));
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }

    public  void  addLifeCycle(String id,String path){
        List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<BucketLifecycleConfiguration.Rule>();
        // 规则1  30天后删除路径以 hongkong_movie/ 为开始的文件
        BucketLifecycleConfiguration.Rule deletePrefixRule = new BucketLifecycleConfiguration.Rule();
        deletePrefixRule.setId(id);
        deletePrefixRule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate(path)));
        // 文件上传或者变更后, 30天后删除
        deletePrefixRule.setExpirationInDays(7);
        // 设置规则为生效状态
        deletePrefixRule.setStatus(BucketLifecycleConfiguration.ENABLED);
        // 规则2  20天后沉降到低频，一年后删除
        BucketLifecycleConfiguration.Rule standardIaRule = new BucketLifecycleConfiguration.Rule();
        standardIaRule.setId(id+ System.currentTimeMillis());
        standardIaRule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate(path)));
        List<BucketLifecycleConfiguration.Transition> standardIaTransitions = new ArrayList<BucketLifecycleConfiguration.Transition>();
        BucketLifecycleConfiguration.Transition standardTransition = new BucketLifecycleConfiguration.Transition();
        standardTransition.setDays(20);
        standardTransition.setStorageClass(StorageClass.Standard_IA.toString());
        standardIaTransitions.add(standardTransition);
        standardIaRule.setTransitions(standardIaTransitions);
        standardIaRule.setStatus(BucketLifecycleConfiguration.ENABLED);
        standardIaRule.setExpirationInDays(30);
        // 将两条规则添加到策略集合中
        rules.add(deletePrefixRule);
//        rules.add(standardIaRule);
        // 生成 bucketLifecycleConfiguration
        BucketLifecycleConfiguration bucketLifecycleConfiguration =
                new BucketLifecycleConfiguration();
        bucketLifecycleConfiguration.setRules(rules);
        // 存储桶的命名格式为 BucketName-APPID
        SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest =
                new SetBucketLifecycleConfigurationRequest(bucketName, bucketLifecycleConfiguration);
        // 设置生命周期
        cosClient.setBucketLifecycleConfiguration(setBucketLifecycleConfigurationRequest);
    }
    public void  queryPath(){
        // 存储桶的命名格式为 BucketName-APPID ，此处填写的存储桶名称必须为此格式
        BucketLifecycleConfiguration queryLifeCycleRet =
                cosClient.getBucketLifecycleConfiguration(bucketName);
        if(queryLifeCycleRet == null){
            return;
        }
        List<BucketLifecycleConfiguration.Rule> ruleLists = queryLifeCycleRet.getRules();
        Iterator<BucketLifecycleConfiguration.Rule> iterator = ruleLists.iterator();
        while (iterator.hasNext()){
            BucketLifecycleConfiguration.Rule next = iterator.next();
            System.out.println("path:" + next.getId());
        }
    }

    public Boolean  deletedAllLifeCycle(){
        COSClient cosClient = COSBuilder();
        cosClient.deleteBucketLifecycleConfiguration(bucketName);
        return  true;
    }

    public static void main(String[] args) throws InterruptedException {
        BucketLifecycleDemo manage = new BucketLifecycleDemo();
        manage.deletedAllLifeCycle();
        manage.addLifeCycle("warehouse-ods-apx","warehouse/ods/apx/");
        manage.addLifeCycle("warehouse-ods-apx2","warehouse/ods/apx2/");
        manage.queryPath();
    }
}