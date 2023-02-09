package com.obs.test.tools;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.Owner;
import com.obs.services.model.Permission;
import com.obs.services.model.RequestPaymentConfiguration;
import com.obs.services.model.RequestPaymentEnum;
import com.obs.services.model.SetBucketAclRequest;
import com.obs.services.model.SetBucketRequestPaymentRequest;

public class BucketTools {
    private static final Logger logger = LogManager.getLogger(BucketTools.class);

    /**
     * 创建桶
     * @param obsClient
     * @param bucketName
     * @return
     */
    public static ObsBucket createBucket(ObsClient obsClient, String bucketName, BucketTypeEnum bucketType) {
        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName("bucketname");
        request.setBucketType(bucketType);
        // 创建桶
        return obsClient.createBucket(bucketName);
    }
    
    /**
     * 设置桶的ACL
     * @param obsClient
     * @param bucketName
     * @param objectKey
     * @param isRecover 是否覆盖
     * @param isRequesterPays 是否开启请求者付费头域
     */
    public static HeaderResponse setBucketAcl(ObsClient obsClient, String ownerId, String userId, String bucketName, 
            boolean isRecover, boolean isRequesterPays) {
        
        AccessControlList accessControlList = null;
        if(isRecover) {
            accessControlList = obsClient.getBucketAcl(bucketName);
        } else {
            accessControlList = new AccessControlList();
            Owner owner = new Owner();
            owner.setId(ownerId);
            accessControlList.setOwner(owner);
        }
        
        accessControlList.grantPermission(new CanonicalGrantee(userId), Permission.PERMISSION_FULL_CONTROL, true);
        
        SetBucketAclRequest request = new SetBucketAclRequest(bucketName, accessControlList);
        request.setRequesterPays(isRequesterPays);
        return obsClient.setBucketAcl(request);
    }
    
    /**
     * 设置桶策略
     * @param srcUser
     * @param bucketName
     * @param policy
     */
    public static void setBucketPolicy (ObsClient obsClient, String bucketName, String policy) {
        obsClient.setBucketPolicy(bucketName, policy);
    }
    
    /**
     * 删除桶策略
     * @param srcUser
     * @param bucketName
     * @param policy
     */
    public static void deleteBucketPolicy (ObsClient obsClient, String bucketName) {
        obsClient.deleteBucketPolicy(bucketName);
    }
    
    /**
     * 删除桶
     *
     * @param forceDelete 是否强制删除，true：强制删除（如果非空，责先删除对象后再删除）
     */
    public static void deleteBucket(ObsClient obsClient, String bucketName, boolean forceDelete) {
        logger.warn("delete bucket : " + bucketName);
        try {
            if(forceDelete) {
                clearBucket(obsClient, bucketName);
            }
            
            obsClient.deleteBucket(bucketName);
        } catch (ObsException e) {
            if(e.getResponseCode() == 404) {
                logger.warn("bucket : " + bucketName + " not exists.");
            } else {
                throw e;
            }
        }
    }

    /**
     * 清空桶，将桶内的对象、未合并的分段对象都删除掉
     * @param obsClient
     * @param bucketName
     */
    public static void clearBucket(ObsClient obsClient, String bucketName) {
        // 列举所有对象，并删除
        ObjectTools.deleteAllObjects(obsClient, bucketName);

        // 取消所有分段任务
        MultipartTools.abortAllMultipartUpload(obsClient, bucketName);

        // 删除所有多版本对象
        ObjectTools.deleteAllVersions(obsClient, bucketName);
    }

    /**
     * 列举所有桶
     *
     * @param obsClient
     * @return
     */
    public static List<ObsBucket> listAllBucket(ObsClient obsClient) {
        ListBucketsRequest request = new ListBucketsRequest();
        request.setQueryLocation(true);
        return obsClient.listBuckets(request);
    }
    
    /**
     * 设置请求者这付费状态
     * @param now
     */
    public static HeaderResponse setBucketRequestPayment(ObsClient obsClient, String bucketName, RequestPaymentEnum now, boolean isRequesterPays) {
        SetBucketRequestPaymentRequest request = new SetBucketRequestPaymentRequest(bucketName, now);
        request.setRequesterPays(isRequesterPays);
        return obsClient.setBucketRequestPayment(request);
    }
    
    /**
     * 设置请求者这付费状态
     * @param now
     */
    public static RequestPaymentConfiguration getBucketRequestPayment(ObsClient obsClient, String bucketName, boolean isRequesterPays) {
        BaseBucketRequest request = new BaseBucketRequest(bucketName);
        request.setRequesterPays(isRequesterPays);
        return obsClient.getBucketRequestPayment(request);
    }
}
