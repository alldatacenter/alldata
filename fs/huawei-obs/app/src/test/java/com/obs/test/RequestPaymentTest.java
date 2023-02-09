package com.obs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AppendObjectRequest;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketDirectColdAccess;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.BucketTagInfo.TagSet;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
import com.obs.services.model.GenericRequest;
import com.obs.services.model.GetObjectAclRequest;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ModifyObjectRequest;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.Permission;
import com.obs.services.model.ProtocolEnum;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.PutObjectsRequest;
import com.obs.services.model.Redirect;
import com.obs.services.model.RenameObjectRequest;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RequestPaymentConfiguration;
import com.obs.services.model.RequestPaymentEnum;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectsRequest;
import com.obs.services.model.RestoreTierEnum;
import com.obs.services.model.RouteRule;
import com.obs.services.model.RouteRuleCondition;
import com.obs.services.model.RuleStatusEnum;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.SetBucketAclRequest;
import com.obs.services.model.SetBucketCorsRequest;
import com.obs.services.model.SetBucketDirectColdAccessRequest;
import com.obs.services.model.SetBucketEncryptionRequest;
import com.obs.services.model.SetBucketLifecycleRequest;
import com.obs.services.model.SetBucketLoggingRequest;
import com.obs.services.model.SetBucketNotificationRequest;
import com.obs.services.model.SetBucketPolicyRequest;
import com.obs.services.model.SetBucketQuotaRequest;
import com.obs.services.model.SetBucketReplicationRequest;
import com.obs.services.model.SetBucketStoragePolicyRequest;
import com.obs.services.model.SetBucketTaggingRequest;
import com.obs.services.model.SetBucketVersioningRequest;
import com.obs.services.model.SetBucketWebsiteRequest;
import com.obs.services.model.SetObjectAclRequest;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.TaskProgressStatus;
import com.obs.services.model.TruncateObjectRequest;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
import com.obs.services.model.VersioningStatusEnum;
import com.obs.services.model.WebsiteConfiguration;
import com.obs.services.model.fs.DropFolderRequest;
import com.obs.services.model.fs.FSStatusEnum;
import com.obs.services.model.fs.GetBucketFSStatusRequest;
import com.obs.services.model.fs.NewFileRequest;
import com.obs.services.model.fs.NewFolderRequest;
import com.obs.services.model.fs.ReadFileRequest;
import com.obs.services.model.fs.ReadFileResult;
import com.obs.services.model.fs.RenameRequest;
import com.obs.services.model.fs.SetBucketFSStatusRequest;
import com.obs.services.model.fs.TruncateFileRequest;
import com.obs.services.model.fs.WriteFileRequest;
import com.obs.test.objects.BaseObjectTest;
import com.obs.test.tools.BucketTools;
import com.obs.test.tools.FileTools;
import com.obs.test.tools.ObjectTools;

/**
 * 405： <br>
<br> test_get_bucket_tagging
<br> test_delete_bucket_tagging
<br> test_set_bucket_tagging
<br> test_rename_object
<br> test_rename_file
<br> test_rename_folder
<br> test_delete_bucket_direct_cold_access
<br> test_set_bucket_direct_cold_access
<br> test_get_bucket_direct_cold_access
<br> test_get_bucket_storage_policy
<br> test_set_bucket_storage_policy
<br> test_restore_object
 *
 */

public class RequestPaymentTest extends BaseObjectTest {

    private static final Logger logger = LogManager.getLogger(RequestPaymentTest.class);
    
    static UserInfo owner;
    static UserInfo requester; 

    static String bucketName_obs;
    static String bucketName_pfs;

public static class CaseInfo {
        
        private int expectSuccessCode;
        private GenericRequest request;
        private String bucketName;

        private UserInfo owner;
        private UserInfo requester;
        
        public CaseInfo(UserInfo _owner, UserInfo _requester, String _bucketName, GenericRequest _request,
                int _expectSuccessCode) {
            this.expectSuccessCode = _expectSuccessCode;
            this.request = _request;
            this.bucketName = _bucketName;
            this.owner = _owner;
            this.requester = _requester;
        }
        
        public int getExpectSuccessCode() {
            return expectSuccessCode;
        }

        public GenericRequest getRequest() {
            return request;
        }

        public String getBucketName() {
            return bucketName;
        }

        public UserInfo getOwner() {
            return owner;
        }

        public UserInfo getRequester() {
            return requester;
        }
    }
    
    abstract class TestRequestPayment<T> extends LogableCase {
        private CaseInfo caseInfo;

        public TestRequestPayment(CaseInfo caseInfo, Logger logger, String caseName) {
            super(logger, caseName);
            this.caseInfo = caseInfo;
        }

        public void doBeforeAction() {
        }
        
        abstract T action(ObsClient obsClient);

//        public void doAfterException(ObsException e) {
//
//        }

        /**
         * 当前关闭请求者付费的时候，实施的检测
         * @param response
         */
        public void checkResultWhilePaymentByOwner(T response) {
            if(response instanceof HeaderResponse) {
                assertEquals(this.getExpectSuccessCode(), ((HeaderResponse)response).getStatusCode());
            }
        }
        
        /**
         * 当前开启请求者付费的时候，实施的检测
         * @param response
         */
        public void checkResultWhilePaymentByRequester(T response) {
            if(response instanceof HeaderResponse) {
                assertRequestPayerResult(this.getExpectSuccessCode(), ((HeaderResponse)response));
            }
        }
        
        /**
         * 当前开启请求者付费，失败之后的检测
         * @param response
         */
        public void checkPaymentByRequesterException(ObsException exception) {
            assertNotNull(exception);
            exception = isRequestPayerDeniedException(exception);
        }
        
        /**
         * 执行测试
         */
        public final void action() {
            // step-1：关闭请求者付费，通过其他请求者来请求的时候，请求正常
            BucketTools.setBucketRequestPayment(this.getOwner().getObsClient(), this.getBucketName(),
                    RequestPaymentEnum.BUCKET_OWNER, false);
            this.getLogger().info("close bucket request payment.");
            
            doBeforeAction();
            this.getLogger().info("do case : " + this.getCaseName() + " while bucket requestpayment is closed , and request parameter is : " + this.getRequest());
            T response = this.action(this.getRequester().getObsClient());
            this.getLogger().info("do case : " + this.getCaseName() + " while bucket requestpayment is closed , and result is : " + response);
            checkResultWhilePaymentByOwner(response);
//            doAfterAction(response);
            
            // step-2：开启请求者付费，不设置头域，请求预期收到403 RequestPayerDenied
            BucketTools.setBucketRequestPayment(this.getOwner().getObsClient(), this.getBucketName(),
                    RequestPaymentEnum.REQUESTER, false);
            this.getLogger().info("open bucket request payment.");
            
            ObsException exception = null;
            try {
                this.getRequest().setRequesterPays(false);
                this.getLogger().info("do case : " + this.getCaseName() + " while bucket requestpayment is open , and request parameter is : " + this.getRequest());
                
                doBeforeAction();
                response = this.action(this.getRequester().getObsClient());
            } catch (ObsException e) {
                exception = e;
            }
            checkPaymentByRequesterException(exception);
//            doAfterException(exception);

            // step-3：开启请求者付费，设置头域，请求预期正常
            this.getRequest().setRequesterPays(true);
            doBeforeAction();
            this.getLogger().info("do case : " + this.getCaseName() + " while bucket requestpayment is open , and request parameter is : " + this.getRequest());
            response = this.action(this.getRequester().getObsClient());
            checkResultWhilePaymentByRequester(response);
            this.getLogger().info("do case : " + this.getCaseName() + " while bucket requestpayment is open , and result is : " + response);
//            doAfterAction(response);
        }
        
        public int getExpectSuccessCode() {
            return this.caseInfo.getExpectSuccessCode();
        }

        public GenericRequest getRequest() {
            return this.caseInfo.getRequest();
        }

        public String getBucketName() {
            return this.caseInfo.getBucketName();
        }

        public UserInfo getOwner() {
            return this.caseInfo.getOwner();
        }

        public UserInfo getRequester() {
            return this.caseInfo.getRequester();
        }
    }
    
    @BeforeClass
    public static void init() {

        owner = new UserInfo(TestTools.getRequestPaymentEnvironment_User1(), "domainiddomainiddomainiddo000012",
                "xxxxxxxxxxxxxxxxxxx");
        requester = new UserInfo(TestTools.getRequestPaymentEnvironment_User2(), "domainiddomainiddomainiddo000011",
                "xxxxxxxxxxxxxxxxxxxxx");

        bucketName_obs = RequestPaymentTest.class.getName().replaceAll("\\.", "-").toLowerCase() + "-obs";
        
        bucketName_pfs = RequestPaymentTest.class.getName().replaceAll("\\.", "-").toLowerCase() + "-pfs";

        // 强制上一个桶
        BucketTools.deleteBucket(owner.getObsClient(), bucketName_obs, true);
        BucketTools.deleteBucket(owner.getObsClient(), bucketName_pfs, true);

        // 创建桶
        BucketTools.createBucket(owner.getObsClient(), bucketName_obs, BucketTypeEnum.OBJECT);
        BucketTools.createBucket(owner.getObsClient(), bucketName_pfs, BucketTypeEnum.PFS);

        // 设置桶权限
        BucketTools.setBucketAcl(owner.getObsClient(), owner.getDomainId(), requester.getDomainId(), bucketName_obs, true,
                false);
        BucketTools.setBucketAcl(owner.getObsClient(), owner.getDomainId(), requester.getDomainId(), bucketName_pfs, true,
                false);
    }

    // @AfterClass
    public static void clear() {
        // 强制删除一个桶
        BucketTools.deleteBucket(owner.getObsClient(), bucketName_obs, true);
        BucketTools.deleteBucket(owner.getObsClient(), bucketName_pfs, true);
    }

    @Before
    public void beforeCase() {
        // 设置桶权限
        logger.info("set all bucket policy");
        setAllBucketPolicy(owner, requester, bucketName_obs);
        setAllBucketPolicy(owner, requester, bucketName_pfs);
    }

    @After
    public void afterCase() {
        // 删除桶策略
        logger.info("delete all bucket policy");
        deleteBucketPolicy(owner, bucketName_obs);
        deleteBucketPolicy(owner, bucketName_pfs);

        // 关闭桶的请求者付费
        logger.info("close bucket request payment");
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.BUCKET_OWNER, false);
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_pfs, RequestPaymentEnum.BUCKET_OWNER, false);
    }

    /**
     * 验证是否是请求者付费的异常，并将异常设置为null
     * 
     * @param exception
     */
    private ObsException isRequestPayerDeniedException(ObsException exception) {
        if(null != exception.getErrorCode()) {
            assertEquals("RequestPayerDenied", exception.getErrorCode());
        } else {
            assertEquals("RequestPayerDenied", exception.getResponseHeaders().get("error-code"));
        }
        
        return null;
    }

    private void assertRequestPayerResult(int expectCode, HeaderResponse result) {
        assertEquals(expectCode, result.getStatusCode());
        assertEquals("requester", result.getResponseHeaders().get("request-charged"));
    }

    @Test
    public void test_set_get_bucket_request_payment_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.BUCKET_OWNER, false);
        RequestPaymentConfiguration config = BucketTools.getBucketRequestPayment(owner.getObsClient(), bucketName_obs,
                false);
        assertEquals(config.getPayer(), RequestPaymentEnum.BUCKET_OWNER);

        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);
        config = BucketTools.getBucketRequestPayment(owner.getObsClient(), bucketName_obs, false);
        assertEquals(config.getPayer(), RequestPaymentEnum.REQUESTER);
    }

    /**
     * 
     */
    @Test
    public void test_set_get_bucket_request_payment_by_requester() {
        setBucketRequestPaymentPolicy(owner, requester, bucketName_obs);
        BucketTools.setBucketRequestPayment(requester.getObsClient(), bucketName_obs, RequestPaymentEnum.BUCKET_OWNER,
                false);
        RequestPaymentConfiguration config = BucketTools.getBucketRequestPayment(requester.getObsClient(), bucketName_obs,
                false);
        assertEquals(config.getPayer(), RequestPaymentEnum.BUCKET_OWNER);

        BucketTools.setBucketRequestPayment(requester.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        ObsException exception = null;
        try {
            BucketTools.setBucketRequestPayment(requester.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER,
                    false);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        HeaderResponse result = BucketTools.setBucketRequestPayment(requester.getObsClient(), bucketName_obs,
                RequestPaymentEnum.REQUESTER, true);
        assertRequestPayerResult(200, result);

        exception = null;
        try {
            config = BucketTools.getBucketRequestPayment(requester.getObsClient(), bucketName_obs, false);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        config = BucketTools.getBucketRequestPayment(requester.getObsClient(), bucketName_obs, true);
        assertEquals(config.getPayer(), RequestPaymentEnum.REQUESTER);
    }

    /**
     * CaseName：未开启请求者付费的情况下，上传对象<br>
     * Step.：<br>
     * 1、关闭请求者付费；<br>
     * 2、采用requester上传对象； <br>
     * 3、预期成功<br>
     */
    @Test
    public void test_put_object_on_bucket_requestpayment_off() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.BUCKET_OWNER, false);

        PutObjectResult result = null;
        try {
            String objectKey = "test_put_object_on_bucket_requestpayment_off";
            result = generateTestObject(requester, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        } catch (ObsException e) {
            e.printStackTrace();
        }

        assertEquals(200, result.getStatusCode());
    }

    /**
     * CaseName：开启请求者付费的情况下，上传对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、采用requester上传对象，并且不设置请求者付费头域； <br>
     * 3、预期失败：403 RequestPayerDenied<br>
     */
    @Test
    public void test_put_object_on_bucket_requestpayment_on_and_request_off() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        PutObjectResult result = null;
        ObsException exception = null;
        try {
            String objectKey = "test_put_object_on_bucket_requestpayment_on_and_request_off";
            result = generateTestObject(requester, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        } catch (ObsException e) {
            exception = e;
        }

        exception = isRequestPayerDeniedException(exception);
    }

    /**
     * CaseName：开启请求者付费的情况下，上传对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、采用requester上传对象，并且设置请求者付费头域； <br>
     * 3、预期成功 <br>
     */
    @Test
    public void test_put_object_on_bucket_requestpayment_on_and_request_on() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        PutObjectResult result = null;
        try {
            String objectKey = "test_put_object_on_bucket_requestpayment_on_and_request_on";
            result = generateTestObject(requester, bucketName_obs, objectKey, true, 1 * 1024 * 1024);
        } catch (ObsException e) {
            e.printStackTrace();
        }

        assertRequestPayerResult(200, result);
    }

    /**
     * CaseName：删除对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过owner上传一个对象； <br>
     * 3、通过requester删除对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester删除对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_delete_object_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_delete_object_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);

        ObsException exception = null;
        DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName_obs, objectKey, null);
        try {
            requester.getObsClient().deleteObject(deleteRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        deleteRequest.setRequesterPays(true);
        DeleteObjectResult deleteResult = requester.getObsClient().deleteObject(deleteRequest);
        assertRequestPayerResult(204, deleteResult);
    }

    /**
     * CaseName：删除对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过requester上传一个对象； <br>
     * 3、通过requester删除对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester删除对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_delete_object_and_object_create_by_requester() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_delete_object_and_object_create_by_requester";

        PutObjectResult putResult = generateTestObject(requester, bucketName_obs, objectKey, true, 1 * 1024 * 1024);
        assertRequestPayerResult(200, putResult);

        ObsException exception = null;
        DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName_obs, objectKey, null);
        try {
            requester.getObsClient().deleteObject(deleteRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        deleteRequest.setRequesterPays(true);
        DeleteObjectResult deleteResult = requester.getObsClient().deleteObject(deleteRequest);
        assertRequestPayerResult(204, deleteResult);
    }

    /**
     * CaseName：批量删除对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过owner上传两个个对象； <br>
     * 3、通过requester批量删除对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester批量删除对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_batch_delete_object_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_batch_delete_object_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey + "-1", false, 1 * 1024 * 1024);
        generateTestObject(owner, bucketName_obs, objectKey + "-2", false, 1 * 1024 * 1024);

        ObsException exception = null;
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName_obs);
        deleteRequest.addKeyAndVersion(objectKey + "-1");
        deleteRequest.addKeyAndVersion(objectKey + "-2");
        try {
            requester.getObsClient().deleteObjects(deleteRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        deleteRequest.setRequesterPays(true);
        DeleteObjectsResult deleteResult = requester.getObsClient().deleteObjects(deleteRequest);
        assertRequestPayerResult(200, deleteResult);
    }

    /**
     * CaseName：批量删除对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过requester上传两个个对象； <br>
     * 3、通过requester批量删除对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester批量删除对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_batch_delete_object_and_object_create_by_requester() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_batch_delete_object_and_object_create_by_requester()";

        PutObjectResult putResult = generateTestObject(requester, bucketName_obs, objectKey + "-1", true, 1 * 1024 * 1024);
        assertRequestPayerResult(200, putResult);

        putResult = generateTestObject(requester, bucketName_obs, objectKey + "-2", true, 1 * 1024 * 1024);
        assertRequestPayerResult(200, putResult);

        ObsException exception = null;
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName_obs);
        deleteRequest.addKeyAndVersion(objectKey + "-1");
        deleteRequest.addKeyAndVersion(objectKey + "-2");
        try {
            requester.getObsClient().deleteObjects(deleteRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        deleteRequest.setRequesterPays(true);
        DeleteObjectsResult deleteResult = requester.getObsClient().deleteObjects(deleteRequest);
        assertRequestPayerResult(200, deleteResult);
    }

    /**
     * CaseName：列举对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过owner上传两个个对象； <br>
     * 3、通过requester列举对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester列举对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_list_object_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_list_object_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey + "-1", false, 1 * 1024 * 1024);
        generateTestObject(owner, bucketName_obs, objectKey + "-2", false, 1 * 1024 * 1024);

        ObsException exception = null;
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName_obs);
        try {
            requester.getObsClient().listObjects(listObjectsRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        listObjectsRequest.setRequesterPays(true);
        ObjectListing listResult = requester.getObsClient().listObjects(listObjectsRequest);
        assertRequestPayerResult(200, listResult);
    }

    /**
     * CaseName：列举对象<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过requester上传两个个对象； <br>
     * 3、通过requester列举对象，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester列举对象，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_list_object_and_object_create_by_requester() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_list_object_and_object_create_by_requester()";

        PutObjectResult putResult = generateTestObject(requester, bucketName_obs, objectKey + "-1", true, 1 * 1024 * 1024);
        assertEquals(200, putResult.getStatusCode());
        assertEquals("requester", putResult.getResponseHeaders().get("request-charged"));

        putResult = generateTestObject(requester, bucketName_obs, objectKey + "-2", true, 1 * 1024 * 1024);
        assertRequestPayerResult(200, putResult);

        ObsException exception = null;
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName_obs);
        try {
            requester.getObsClient().listObjects(listObjectsRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        listObjectsRequest.setRequesterPays(true);
        ObjectListing listResult = requester.getObsClient().listObjects(listObjectsRequest);
        assertRequestPayerResult(200, listResult);
    }

    /**
     * CaseName：获取对象acl<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过owner上传一个对象； <br>
     * 3、通过owner给requester设置acl权限；<br>
     * 4、通过requester获取acl，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 5、通过requester获取acl，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_get_acl_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_set_acl_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

        GetObjectAclRequest getRequest = new GetObjectAclRequest(bucketName_obs, objectKey, null);
        ObsException exception = null;
        try {
            requester.getObsClient().getObjectAcl(getRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        getRequest.setRequesterPays(true);
        requester.getObsClient().getObjectAcl(getRequest);
    }

    /**
     * CaseName：判断对象是否存在接口<br>
     * Step.：<br>
     * 1、开启请求者付费；<br>
     * 2、通过owner上传一个对象； <br>
     * 3、通过requester调用判断对象是否存在接口，并且不设置请求者付费头域，预期失败：403 RequestPayerDenied <br>
     * 4、通过requester调用判断对象是否存在接口，并且设置请求者付费头域，预期成功。
     */
    @Test
    public void test_check_object_exist_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_check_object_exist_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

        GetObjectMetadataRequest checkRequest = new GetObjectMetadataRequest(bucketName_obs, objectKey);
        Exception exception = null;
        try {
            requester.getObsClient().doesObjectExist(checkRequest);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        exception = null;

        checkRequest.setRequesterPays(true);
        boolean result = requester.getObsClient().doesObjectExist(checkRequest);
        assertTrue(result);
    }

    @Test
    public void test_get_object_metadata_exist_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_get_object_metadata_exist_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest(bucketName_obs, objectKey);
        ObsException exception = null;
        try {
            requester.getObsClient().getObjectMetadata(getRequest);
        } catch (ObsException e) {
            exception = e;
        }
        assertNotNull(exception);
        exception = null;

        getRequest.setRequesterPays(true);
        ObjectMetadata objectMetadata = requester.getObsClient().getObjectMetadata(getRequest);
        assertRequestPayerResult(200, objectMetadata);
    }

    @Test
    public void test_set_object_metadata_exist_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_set_object_metadata_exist_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);
        setModifyObjectMetaDataPolicy(owner, requester, bucketName_obs);

        SetObjectMetadataRequest setRequest = new SetObjectMetadataRequest(bucketName_obs, objectKey);
        setRequest.getMetadata().put("test", "test");
        ObsException exception = null;
        try {
            requester.getObsClient().setObjectMetadata(setRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        setRequest.getMetadata().put("test-requester", "test-requester");
        setRequest.setRequesterPays(true);
        ObjectMetadata metadata = requester.getObsClient().setObjectMetadata(setRequest);
    }

    @Test
    public void test_download_object_and_object_create_by_owner() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_download_object_and_object_create_by_owner";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

        GetObjectRequest getRequest = new GetObjectRequest(bucketName_obs, objectKey);
        ObsException exception = null;
        try {
            requester.getObsClient().getObject(getRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        getRequest.setRequesterPays(true);
        ObsObject obsObject = requester.getObsClient().getObject(getRequest);

        assertEquals("requester", obsObject.getMetadata().getMetadata().get("request-charged"));
    }

    @Test
    public void test_list_versions() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        String objectKey = "test_list_versions";

        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);
        // 给requester 设置权限
        ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

        ListVersionsRequest listRequest = new ListVersionsRequest(bucketName_obs);

        ObsException exception = null;
        try {
            requester.getObsClient().listVersions(listRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        listRequest.setRequesterPays(true);
        ListVersionsResult result = requester.getObsClient().listVersions(listRequest);

        assertRequestPayerResult(200, result);
    }

    @Test
    public void test_copy_object_on_bucket_requestpayment_off() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.BUCKET_OWNER, false);

        CopyObjectResult result = null;
        try {

            String objectKey = "test_copy_object_on_bucket_requestpayment_off";

            generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);

            ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

            result = requester.getObsClient().copyObject(generateCopyObjectRequest(bucketName_obs, objectKey));

        } catch (ObsException e) {
            e.printStackTrace();
        }

        assertEquals(200, result.getStatusCode());
    }

    @Test
    public void test_copy_object_on_bucket_requestpayment_on_and_request_off() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        CopyObjectResult result = null;
        ObsException exception = null;
        try {
            String objectKey = "test_copy_object_on_bucket_requestpayment_on_and_request_off";

            generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);

            ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

            CopyObjectRequest copyRequest = generateCopyObjectRequest(bucketName_obs, objectKey);
            copyRequest.setRequesterPays(false);
            result = requester.getObsClient().copyObject(copyRequest);
        } catch (ObsException e) {
            exception = e;
        }

        exception = isRequestPayerDeniedException(exception);
    }

    @Test
    public void test_copy_object_on_bucket_requestpayment_on_and_request_on() {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        CopyObjectResult result = null;
        try {
            String objectKey = "test_copy_object_on_bucket_requestpayment_on_and_request_on";

            generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024 * 1024);

            ObjectTools.setObjectAcl(owner, requester, bucketName_obs, objectKey);

            CopyObjectRequest copyRequest = generateCopyObjectRequest(bucketName_obs, objectKey);
            copyRequest.setRequesterPays(true);
            result = requester.getObsClient().copyObject(copyRequest);
        } catch (ObsException e) {
            e.printStackTrace();
        }

        assertRequestPayerResult(200, result);
    }

    /**
     * 测试初始化分段、上传分段、列举分段、取消分段、合并分段的请求者付费
     * 
     * @throws IOException
     */
    @Test
    public void test_multipartUpload_object_on_bucket_requestpayment_on_and_request_on() throws IOException {
        BucketTools.setBucketRequestPayment(owner.getObsClient(), bucketName_obs, RequestPaymentEnum.REQUESTER, false);

        ObsException exception = null;
        String objectKey = "test_multipartUpload_object_on_bucket_requestpayment_on_and_request_on";

        ListPartsResult listResult = init_for_test_mulitpartUload(bucketName_obs, objectKey);
        AbortMultipartUploadRequest abortRequest = generateAbortMultipartUploadRequest(bucketName_obs, objectKey,
                listResult.getUploadId());
        try {
            requester.getObsClient().abortMultipartUpload(abortRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        abortRequest.setRequesterPays(true);
        HeaderResponse abortResult = requester.getObsClient().abortMultipartUpload(abortRequest);
        assertRequestPayerResult(204, abortResult);

        listResult = init_for_test_mulitpartUload(bucketName_obs, objectKey);
        CompleteMultipartUploadRequest completeRequest = generateCompleteMultipartUploadRequest(listResult);
        try {
            requester.getObsClient().completeMultipartUpload(completeRequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        completeRequest.setRequesterPays(true);
        assertEquals(listResult.getMultipartList().size(), completeRequest.getPartEtag().size());

        CompleteMultipartUploadResult completeResult = requester.getObsClient()
                .completeMultipartUpload(completeRequest);
        assertRequestPayerResult(200, completeResult);

        assertEquals(null, exception);
    }

    private ListPartsResult init_for_test_mulitpartUload(String bucketName, String objectKey) throws IOException {
        ObsException exception = null;

        InitiateMultipartUploadRequest initrequest = generateInitiateMultipartUploadRequest(bucketName, objectKey);
        try {
            requester.getObsClient().initiateMultipartUpload(initrequest);
        } catch (ObsException e) {
            exception = e;
        }
        assertEquals("RequestPayerDenied", exception.getErrorCode());
        exception = null;

        initrequest.setRequesterPays(true);
        InitiateMultipartUploadResult initResult = requester.getObsClient().initiateMultipartUpload(initrequest);
        assertRequestPayerResult(200, initResult);

        UploadPartRequest uploadParerequest = generateUploadPartRequest(bucketName, objectKey, initResult.getUploadId(),
                1);
        try {
            requester.getObsClient().uploadPart(uploadParerequest);
        } catch (ObsException e) {
            exception = e;
        }
        exception = isRequestPayerDeniedException(exception);

        uploadParerequest = generateUploadPartRequest(bucketName, objectKey, initResult.getUploadId(), 1);
        uploadParerequest.setRequesterPays(true);
        UploadPartResult uploadPartResult = requester.getObsClient().uploadPart(uploadParerequest);
        assertRequestPayerResult(200, uploadPartResult);

        uploadParerequest = generateUploadPartRequest(bucketName, objectKey, initResult.getUploadId(), 2);
        uploadParerequest.setRequesterPays(true);
        uploadPartResult = requester.getObsClient().uploadPart(uploadParerequest);
        assertRequestPayerResult(200, uploadPartResult);

        ListPartsRequest listRequest = generateListPartsRequest(bucketName, objectKey, initResult.getUploadId());
        try {
            requester.getObsClient().listParts(listRequest);
        } catch (ObsException e) {
            exception = e;
        }
        isRequestPayerDeniedException(exception);

        listRequest.setRequesterPays(true);
        ListPartsResult listResult = requester.getObsClient().listParts(listRequest);
        assertRequestPayerResult(200, listResult);
        assertEquals(2, listResult.getMultipartList().size());

        ListMultipartUploadsRequest listMultipartUploadsRequest = generateListMultipartUploadsRequest(bucketName);
        try {
            requester.getObsClient().listMultipartUploads(listMultipartUploadsRequest);
        } catch (ObsException e) {
            exception = e;
        }
        isRequestPayerDeniedException(exception);

        listMultipartUploadsRequest.setRequesterPays(true);
        MultipartUploadListing listMultipartUploadsResult = requester.getObsClient()
                .listMultipartUploads(listMultipartUploadsRequest);
        assertEquals(200, listMultipartUploadsResult.getStatusCode());

        return listResult;
    }

    /**
     * CaseName：删除桶<br>
     * Step.：<br>
     * 1、owner创建一个桶；<br>
     * 2、开启请求者付费；<br>
     * 3、给requester设置权限； <br>
     * 4、不设置请求者付费头域，采用requester删除桶，预期报错：403 RequestPayerDenied <br>
     * 5、设置请求者付费头域，采用requester删除桶，预期成功 <br>
     */
    @Test
    public void test_delete_bucket() {
        String testBucketName = "test-delete-bucket-11111";
        BucketTools.createBucket(owner.getObsClient(), testBucketName, BucketTypeEnum.OBJECT);
        setBucketAcl(owner, requester, testBucketName);
        setAllBucketPolicy(owner, requester, testBucketName);

        BaseBucketRequest request = new BaseBucketRequest(testBucketName);

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, testBucketName, request, 204),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucket((BaseBucketRequest) this.getRequest());
            }

            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                BucketTools.createBucket(this.getOwner().getObsClient(), this.getBucketName(), BucketTypeEnum.OBJECT);
                setBucketAcl(this.getOwner(), this.getRequester(), this.getBucketName());
                setAllBucketPolicy(this.getOwner(), this.getRequester(), this.getBucketName());
            };
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                BucketTools.createBucket(this.getOwner().getObsClient(), this.getBucketName(), BucketTypeEnum.OBJECT);
                setBucketAcl(this.getOwner(), this.getRequester(), this.getBucketName());
                setAllBucketPolicy(this.getOwner(), this.getRequester(), this.getBucketName());
            };
        }.doTest();
    }

    @Test
    public void test_get_bucket_acl() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketAcl((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }

    @Test
    public void test_set_bucket_acl() {
        AccessControlList accessControlList = owner.getObsClient().getBucketAcl(bucketName_obs);

        accessControlList.grantPermission(new CanonicalGrantee(requester.getDomainId()),
                Permission.PERMISSION_FULL_CONTROL, true);

        SetBucketAclRequest request = new SetBucketAclRequest(bucketName_obs, accessControlList);

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketAcl((SetBucketAclRequest) this.getRequest());
            }
        }.doTest();
    }

    @Test
    public void test_get_bucket_version() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketVersioning((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }

    @Test
    public void test_set_bucket_version() {
        SetBucketVersioningRequest request = new SetBucketVersioningRequest(bucketName_obs, VersioningStatusEnum.ENABLED);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketVersioning((SetBucketVersioningRequest) this.getRequest());
            }

            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                BucketVersioningConfiguration status = this.getOwner().getObsClient()
                        .getBucketVersioning(this.getBucketName());
                assertEquals(VersioningStatusEnum.ENABLED, status.getVersioningStatus());
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                BucketVersioningConfiguration status = this.getOwner().getObsClient()
                        .getBucketVersioning(this.getBucketName());
                assertEquals(VersioningStatusEnum.ENABLED, status.getVersioningStatus());
            }
        }.doTest();

        request = new SetBucketVersioningRequest(bucketName_obs, VersioningStatusEnum.SUSPENDED);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketVersioning((SetBucketVersioningRequest) this.getRequest());
            }

            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                BucketVersioningConfiguration status = this.getOwner().getObsClient()
                        .getBucketVersioning(this.getBucketName());
                assertEquals(VersioningStatusEnum.SUSPENDED, status.getVersioningStatus());
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                BucketVersioningConfiguration status = this.getOwner().getObsClient()
                        .getBucketVersioning(this.getBucketName());
                assertEquals(VersioningStatusEnum.SUSPENDED, status.getVersioningStatus());
            }
        }.doTest();
    }

    @Test
    @Ignore
    public void test_get_bucket_tagging() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketTagging((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }

    @Test
    @Ignore
    public void test_set_bucket_tagging() {
        TagSet tagSet = new TagSet();
        tagSet.addTag("test1", "test_tag_1");
        BucketTagInfo bucketTagInfo = new BucketTagInfo(tagSet);
        SetBucketTaggingRequest request = new SetBucketTaggingRequest(bucketName_obs, bucketTagInfo);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketTagging((SetBucketTaggingRequest) this.getRequest());
            }
            
            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                BucketTagInfo tagInfo = this.getOwner().getObsClient()
                        .getBucketTagging(this.getBucketName());
                assertEquals(1, tagInfo.getTagSet().getTags().size());
                assertEquals("test_tag_1", tagInfo.getTagSet().getTags().get(0).getValue());
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                BucketTagInfo tagInfo = this.getOwner().getObsClient()
                        .getBucketTagging(this.getBucketName());
                assertEquals(1, tagInfo.getTagSet().getTags().size());
                assertEquals("test_tag_1", tagInfo.getTagSet().getTags().get(0).getValue());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_delete_bucket_tagging() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketTagging((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_website() {
        WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
        websiteConfig.setSuffix("test.html");
        websiteConfig.setKey("error_test.html");
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName_obs, websiteConfig);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketWebsite((SetBucketWebsiteRequest) this.getRequest());
            }
            
            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                WebsiteConfiguration config = this.getOwner().getObsClient().getBucketWebsite(new BaseBucketRequest(this.getBucketName()));
                assertEquals("test.html", config.getSuffix());
                assertEquals("error_test.html", config.getKey());
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                WebsiteConfiguration config = this.getOwner().getObsClient().getBucketWebsite(new BaseBucketRequest(this.getBucketName()));
                assertEquals("test.html", config.getSuffix());
                assertEquals("error_test.html", config.getKey());
            }
        }.doTest();
    }
    
    @Test
    public void test_delete_bucket_website() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 204), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketWebsite((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
                WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
                websiteConfig.setSuffix("test.html");
                websiteConfig.setKey("error_test.html");
                SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName_obs, websiteConfig);
                this.getOwner().getObsClient().setBucketWebsite(request);
            }
            
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_website() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketWebsite((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
                WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
                websiteConfig.setSuffix("test.html");
                websiteConfig.setKey("error_test.html");
                
                RouteRule rule = new RouteRule();
                Redirect r = new Redirect();
                r.setHostName("www.example.com");
                r.setHttpRedirectCode("305");
                r.setRedirectProtocol(ProtocolEnum.HTTP);
                r.setReplaceKeyPrefixWith("replacekeyprefix");
                rule.setRedirect(r);
                RouteRuleCondition condition = new RouteRuleCondition();
                condition.setHttpErrorCodeReturnedEquals("404");
                condition.setKeyPrefixEquals("keyprefix");
                rule.setCondition(condition);
                websiteConfig.getRouteRules().add(rule);
                
                SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName_obs, websiteConfig);
                this.getOwner().getObsClient().setBucketWebsite(request);
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_set_bucket_replication() {
        ReplicationConfiguration replicationConfiguration = new ReplicationConfiguration();
        SetBucketReplicationRequest request = new SetBucketReplicationRequest(bucketName_obs, replicationConfiguration);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketReplication((SetBucketReplicationRequest) this.getRequest());
            }
            
            @Override
            public void checkResultWhilePaymentByOwner(HeaderResponse response) {
                super.checkResultWhilePaymentByOwner(response);
                ReplicationConfiguration config = this.getOwner().getObsClient().getBucketReplication(new BaseBucketRequest(this.getBucketName()));
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(HeaderResponse response) {
                super.checkResultWhilePaymentByRequester(response);
                ReplicationConfiguration config = this.getOwner().getObsClient().getBucketReplication(new BaseBucketRequest(this.getBucketName()));
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_get_bucket_replication() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketReplication((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
//                this.getOwner().getObsClient().setBucketReplication(request);
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_delete_bucket_replication() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 204), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketReplication((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
//                this.getOwner().getObsClient().setBucketReplication(request);
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_get_bucket_policy() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketPolicyV2((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_delete_bucket_policy() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketPolicy((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_policy() {
        SetBucketPolicyRequest request = new SetBucketPolicyRequest(bucketName_obs, generateS3PolicyString(bucketName_obs, "*", "*"));
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketPolicy((SetBucketPolicyRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_logging() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketLogging((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_logging() {
        BucketLoggingConfiguration loggingConfiguration = new BucketLoggingConfiguration();
        SetBucketLoggingRequest request = new SetBucketLoggingRequest(bucketName_obs, loggingConfiguration);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketLogging((SetBucketLoggingRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_head_bucket() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<Boolean>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            Boolean action(ObsClient obsClient) {
                return new Boolean(obsClient.headBucket((BaseBucketRequest) this.getRequest()));
            }
            
            @Override
            public void checkResultWhilePaymentByOwner(Boolean resule) {
                assertTrue(resule);
            }
            
            @Override
            public void checkResultWhilePaymentByRequester(Boolean resule) {
                assertTrue(resule);
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_lifecycle() {
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName_obs, createLifecycleConfiguration());
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketLifecycle((SetBucketLifecycleRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_lifecycle() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketLifecycle((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName_obs, createLifecycleConfiguration());
                this.getOwner().getObsClient().setBucketLifecycle(request);
            }
        }.doTest();
    }
    
    @Test
    public void test_delete_bucket_lifecycle() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 204), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketLifecycle((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_metadata() {
        BucketMetadataInfoRequest request = new BucketMetadataInfoRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketMetadata((BucketMetadataInfoRequest) this.getRequest());
            }
            
            @Override
            public void checkPaymentByRequesterException(ObsException exception) {
                exception = isRequestPayerDeniedException(exception);
                // TODO
//                assertNotNull(exception);
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_Cors() {
        SetBucketCorsRequest request = new SetBucketCorsRequest(bucketName_obs, createSetBucketCorsRequest());
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketCors((SetBucketCorsRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_Cors() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketCors((BaseBucketRequest) this.getRequest());
            }
            
            @Override
            public void doBeforeAction() {
                SetBucketCorsRequest request = new SetBucketCorsRequest(bucketName_obs, createSetBucketCorsRequest());
                this.getOwner().getObsClient().setBucketCors(request);
            }
        }.doTest();
    }
    
    @Test
    public void test_delete_bucket_Cors() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 204), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketCors((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_set_bucket_storage_policy() {
        BucketStoragePolicyConfiguration config = new BucketStoragePolicyConfiguration(StorageClassEnum.WARM);
        SetBucketStoragePolicyRequest request = new SetBucketStoragePolicyRequest(bucketName_obs, config);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketStoragePolicy((SetBucketStoragePolicyRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_get_bucket_storage_policy() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketStoragePolicy((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_storage_info() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketStorageInfo((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_quota() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketQuota((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_quota() {
        BucketQuota bucketQuota = new BucketQuota(10 * 1024 * 1024 * 1024L);
        SetBucketQuotaRequest request = new SetBucketQuotaRequest(bucketName_obs, bucketQuota);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketQuota((SetBucketQuotaRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_encryption() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);

        try {
            BucketEncryption bucketEncryption = new BucketEncryption(SSEAlgorithmEnum.AES256);
            SetBucketEncryptionRequest setRequest = new SetBucketEncryptionRequest(bucketName_obs, bucketEncryption);
            owner.getObsClient().setBucketEncryption(setRequest);
            
            new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                    logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
                @Override
                HeaderResponse action(ObsClient obsClient) {
                    return obsClient.getBucketEncryption((BaseBucketRequest) this.getRequest());
                }
            }.doTest();
        } finally {
            BaseBucketRequest deleteRequest = new BaseBucketRequest(bucketName_obs);
            owner.getObsClient().deleteBucketEncryption(deleteRequest);
        }
        
    }
    
    @Test
    public void test_set_bucket_encryption() {
        BucketEncryption bucketEncryption = new BucketEncryption(SSEAlgorithmEnum.AES256);
        SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(bucketName_obs, bucketEncryption);
        try {
            new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                    logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
                @Override
                HeaderResponse action(ObsClient obsClient) {
                    return obsClient.setBucketEncryption((SetBucketEncryptionRequest) this.getRequest());
                }
            }.doTest();
        } finally {
            BaseBucketRequest deleteRequest = new BaseBucketRequest(bucketName_obs);
            owner.getObsClient().deleteBucketEncryption(deleteRequest);
        }
    }
    
    @Test
    public void test_delete_bucket_encryption() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 204), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketEncryption((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_location() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketLocation((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_get_bucket_direct_cold_access() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketDirectColdAccess((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_notification() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketNotification((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_bucket_notification() {
        BucketNotificationConfiguration bucketNotificationConfiguration = new BucketNotificationConfiguration();
        
        SetBucketNotificationRequest request = new SetBucketNotificationRequest(bucketName_obs, bucketNotificationConfiguration);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketNotification((SetBucketNotificationRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_set_bucket_direct_cold_access() {
        BucketDirectColdAccess access = new BucketDirectColdAccess(RuleStatusEnum.DISABLED);
        SetBucketDirectColdAccessRequest request = new SetBucketDirectColdAccessRequest(bucketName_obs, access);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketDirectColdAccess((SetBucketDirectColdAccessRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_delete_bucket_direct_cold_access() {
        BaseBucketRequest request = new BaseBucketRequest(bucketName_obs);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.deleteBucketDirectColdAccess((BaseBucketRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_object_acl() {
        String objectKey = "test_object_acl";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        GetObjectAclRequest request = new GetObjectAclRequest(bucketName_obs, objectKey, null);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getObjectAcl((GetObjectAclRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_set_object_acl() {
        String objectKey = "test_object_acl_set";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        AccessControlList accessControlList = owner.getObsClient().getObjectAcl(bucketName_obs, objectKey);
        accessControlList.grantPermission(new CanonicalGrantee(requester.getDomainId()),
                Permission.PERMISSION_FULL_CONTROL, true);

        SetObjectAclRequest request = new SetObjectAclRequest(bucketName_obs, objectKey, accessControlList);

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setObjectAcl((SetObjectAclRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_rename_object() {
        String objectKey = "test_rename_object_before";
        String newObjectKey = "test_rename_object_before_after_name";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        
        RenameObjectRequest request = new RenameObjectRequest(bucketName_obs, objectKey, newObjectKey);

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.renameObject((RenameObjectRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_modify_object() {
        String objectKey = "test_modify_object_before";
        generateTestObject(owner, bucketName_obs, objectKey, false, 2 * 1024);
        
        ModifyObjectRequest request = new ModifyObjectRequest(bucketName_obs, objectKey, new ByteArrayInputStream(getByte(3 * 1024)));

        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.modifyObject((ModifyObjectRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_append_object() {
        String objectKey = "test_append_object_before";
        // bucketName, objectKey, new ByteArrayInputStream(getByte(3 * 1024))
        AppendObjectRequest request = new AppendObjectRequest();
        request.setBucketName(bucketName_obs);
        request.setObjectKey(objectKey);
        request.setPosition(0);
        request.setInput(new ByteArrayInputStream(getByte(2 * 1024)));
        owner.getObsClient().appendObject(request);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                AppendObjectRequest appendRequest = (AppendObjectRequest)this.getRequest();
                ObjectMetadata metadata = this.getOwner().getObsClient().getObjectMetadata(appendRequest.getBucketName(), appendRequest.getObjectKey());
                
                appendRequest.setPosition(metadata.getNextPosition());
                appendRequest.setInput(new ByteArrayInputStream(getByte(1 * 1024)));
                return obsClient.appendObject(appendRequest);
            }
        }.doTest();
    }
    
    @Test
    public void test_append_file() {
        String objectKey = "test_append_file_before";
        // bucketName, objectKey, new ByteArrayInputStream(getByte(3 * 1024))
        AppendObjectRequest request = new AppendObjectRequest();
        request.setBucketName(bucketName_obs);
        request.setObjectKey(objectKey);
        request.setPosition(0);
        request.setInput(new ByteArrayInputStream(getByte(2 * 1024)));
        owner.getObsClient().appendObject(request);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                AppendObjectRequest appendRequest = (AppendObjectRequest)this.getRequest();
                ObjectMetadata metadata = this.getOwner().getObsClient().getObjectMetadata(appendRequest.getBucketName(), appendRequest.getObjectKey());
                
                appendRequest.setPosition(metadata.getNextPosition());
                appendRequest.setInput(new ByteArrayInputStream(getByte(1 * 1024)));
                return obsClient.appendObject(appendRequest);
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_restore_object() {
        String objectKey = "test_restore_object_before";
        generateTestObject(owner, bucketName_obs, objectKey, false, 2 * 1024);
        
        RestoreObjectRequest request = new RestoreObjectRequest(bucketName_obs, objectKey, 15);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.restoreObjectV2((RestoreObjectRequest)this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_restore_objects() {
        String objectKey = "test_restore_objects";
        generateTestObject(owner, bucketName_obs, objectKey, false, 2 * 1024);
        
        RestoreObjectsRequest request = new RestoreObjectsRequest(bucketName_obs, 15, RestoreTierEnum.STANDARD);
        
        new TestRequestPayment<TaskProgressStatus>(new CaseInfo(owner, requester, bucketName_obs, request, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            TaskProgressStatus action(ObsClient obsClient) {
                return obsClient.restoreObjects((RestoreObjectsRequest)this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_options_object() {
        final String objectKey = "test_options_object";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        
        String allowOrigin = "http://baidu.com";
        List<String> allowHeaders = new ArrayList<String>();
        allowHeaders.add("x-obs-header");
        
        List<String> allowedMethod = new ArrayList<String>();
        // 指定允许的跨域请求方法(GET/PUT/DELETE/POST/HEAD)
        allowedMethod.add("GET");
        allowedMethod.add("HEAD");
        allowedMethod.add("PUT");
        
        List<String> exposeHeaders = new ArrayList<String>();
        // 指定允许用户从应用程序中访问的header
        exposeHeaders.add("x-obs-expose-header");
        
        OptionsInfoRequest optionInfo = new OptionsInfoRequest();
        optionInfo.setOrigin(allowOrigin);
        optionInfo.setRequestHeaders(allowHeaders);
        optionInfo.setRequestMethod(allowedMethod);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, optionInfo, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.optionsObject(bucketName_obs, objectKey, (OptionsInfoRequest)this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_options_bucket() {
        String objectKey = "test_options_object";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        
        String allowOrigin = "http://baidu.com";
        List<String> allowHeaders = new ArrayList<String>();
        allowHeaders.add("x-obs-header");
        
        List<String> allowedMethod = new ArrayList<String>();
        // 指定允许的跨域请求方法(GET/PUT/DELETE/POST/HEAD)
        allowedMethod.add("GET");
        allowedMethod.add("HEAD");
        allowedMethod.add("PUT");
        
        List<String> exposeHeaders = new ArrayList<String>();
        // 指定允许用户从应用程序中访问的header
        exposeHeaders.add("x-obs-expose-header");
        
        OptionsInfoRequest optionInfo = new OptionsInfoRequest();
        optionInfo.setOrigin(allowOrigin);
        optionInfo.setRequestHeaders(allowHeaders);
        optionInfo.setRequestMethod(allowedMethod);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, optionInfo, 200),
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.optionsBucket(bucketName_obs, (OptionsInfoRequest)this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_get_bucket_fsstatus() {
        GetBucketFSStatusRequest request = new GetBucketFSStatusRequest(bucketName_obs);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.getBucketFSStatus((GetBucketFSStatusRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_set_bucket_fsstatus() {
        SetBucketFSStatusRequest request = new SetBucketFSStatusRequest(bucketName_obs, FSStatusEnum.ENABLED);
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.setBucketFSStatus((SetBucketFSStatusRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_new_file() {
        String objectKey = "test_new_file";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        NewFileRequest request = new NewFileRequest(bucketName_pfs, objectKey, new ByteArrayInputStream(getByte(1 * 1024)));
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.newFile((NewFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_read_file() {
        String objectKey = "test_read_file";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        ReadFileRequest request = new ReadFileRequest(bucketName_pfs, objectKey);
        
        new TestRequestPayment<ReadFileResult>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            ReadFileResult action(ObsClient obsClient) {
                return obsClient.readFile((ReadFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    /**
     * 关闭断点续传
     */
    @Test
    public void test_download_file_checkpoint_false() {
        String objectKey = "test_download_file_checkpoint_false";
        generateTestObject(owner, bucketName_obs, objectKey, false, 10 * 1024 * 1024);
        DownloadFileRequest request = new DownloadFileRequest(bucketName_obs, objectKey);
        request.setEnableCheckpoint(false);
        
        new TestRequestPayment<DownloadFileResult>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            DownloadFileResult action(ObsClient obsClient) {
                return obsClient.downloadFile((DownloadFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    /**
     * 开启断点续传
     */
    @Test
    public void test_download_file_checkpoint_true() {
        String objectKey = "test_download_file_checkpoint_true";
        generateTestObject(owner, bucketName_obs, objectKey, false, 10 * 1024 * 1024);
        DownloadFileRequest request = new DownloadFileRequest(bucketName_obs, objectKey);
        request.setEnableCheckpoint(true);
        
        new TestRequestPayment<DownloadFileResult>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            DownloadFileResult action(ObsClient obsClient) {
                return obsClient.downloadFile((DownloadFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_rename_file() {
        String objectKey = "test_rename_file";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        RenameRequest request = new RenameRequest(bucketName_pfs, objectKey, objectKey + "_after_rename");
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.renameFile((RenameRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_upload_file_checkpoint_false() throws IOException {
        String objectKey = "test_upload_file";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        
        File file = FileTools.createALocalTempFile(objectKey+"_local_file", 10 * 1024 * 1024);
        
        UploadFileRequest request = new UploadFileRequest(bucketName_obs, objectKey, file.getPath());
        
        request.setEnableCheckpoint(false);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.uploadFile((UploadFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_upload_file_checkpoint_true() throws IOException {
        String objectKey = "test_upload_file";
        generateTestObject(owner, bucketName_obs, objectKey, false, 1 * 1024);
        
        File file = FileTools.createALocalTempFile(objectKey+"_local_file", 10 * 1024 * 1024);
        
        UploadFileRequest request = new UploadFileRequest(bucketName_obs, objectKey, file.getPath());
        
        request.setEnableCheckpoint(true);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.uploadFile((UploadFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_write_file() throws IOException {
        String objectKey = "test_write_file";
        
        WriteFileRequest request = new WriteFileRequest(bucketName_obs, objectKey, new ByteArrayInputStream(getByte(1 * 1024)));
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.writeFile((WriteFileRequest)this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_new_folder() {
        String objectKey = "test_new_folder";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        NewFolderRequest request = new NewFolderRequest(bucketName_pfs, objectKey);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.newFolder((NewFolderRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_rename_folder() {
        String objectKey = "test_rename_folder";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        RenameRequest request = new RenameRequest(bucketName_pfs, objectKey, objectKey + "_after_rename");
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.renameFolder((RenameRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    public void test_drop_folder() {
        final String folder = "test_drop_folder/";
        
        DropFolderRequest request = new DropFolderRequest(bucketName_pfs, folder);
        
        new TestRequestPayment<TaskProgressStatus>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            TaskProgressStatus action(ObsClient obsClient) {
                for(int i=0; i<5; i++) {
                    generateTestObject(owner, bucketName_pfs, folder + i, false, 1 * 1);
                    for(int j=0; j<5; j++) {
                        generateTestObject(owner, bucketName_pfs, folder + i + "/" + j, false, 1 * 1);
                    }
                }
                
                return obsClient.dropFolder((DropFolderRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    // 批量上传接口还有问题，上传失败后，无法将错误信息抛出来
    public void test_put_objects_by_filepath() throws IOException {
        List<String> paths = new ArrayList<String>();
        String path = "";
        for(int i=1; i<=3; i++) {
            path = "test_put_objects/" + i + "_i";
            paths.add(path);
            
            FileTools.createALocalTempFile(path, 1);
            for(int j=1; j<=3; j++) {
                path = "test_put_objects/" + i + "path/" + j + "_j";
                paths.add(path);
                
                FileTools.createALocalTempFile(path, 1);
            }
        }
        
        PutObjectsRequest request = new PutObjectsRequest(bucketName_obs, paths);
        new TestRequestPayment<TaskProgressStatus>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            TaskProgressStatus action(ObsClient obsClient) {
                return obsClient.putObjects((PutObjectsRequest) this.getRequest());
            }
            
            @Override
            public void checkResultWhilePaymentByOwner(TaskProgressStatus response) {
                super.checkResultWhilePaymentByOwner(response);
                BucketTools.clearBucket(owner.getObsClient(), bucketName_obs);
            };
        }.doTest();
    }
    
    @Test
    @Ignore
    // 批量上传接口还有问题，上传失败后，无法将错误信息抛出来
    public void test_put_objects_by_folderpath() throws IOException {
        String folder = "test_put_objects";
        List<String> paths = new ArrayList<String>();
        String path = "";
        for(int i=1; i<=3; i++) {
            path = folder + "/" + i + "_i";
            paths.add(path);
            
            FileTools.createALocalTempFile(path, 1);
            for(int j=1; j<=3; j++) {
                path = folder + "/" + i + "path/" + j + "_j";
                paths.add(path);
                
                FileTools.createALocalTempFile(path, 1);
            }
        }
        
        PutObjectsRequest request = new PutObjectsRequest(bucketName_obs, folder);
        new TestRequestPayment<TaskProgressStatus>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            TaskProgressStatus action(ObsClient obsClient) {
                return obsClient.putObjects((PutObjectsRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
    @Ignore
    public void test_truncate_file() {
        String objectKey = "test_truncate_file";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        TruncateFileRequest request = new TruncateFileRequest(bucketName_pfs, objectKey, 512);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_pfs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.truncateFile((TruncateFileRequest) this.getRequest());
            }
        }.doTest();
    }
    
    @Test
//    @Ignore
    public void test_truncate_object() {
        String objectKey = "test_truncate_file";
        generateTestObject(owner, bucketName_pfs, objectKey, false, 1 * 1024);
        TruncateObjectRequest request = new TruncateObjectRequest(bucketName_obs, objectKey, 512);
        
        new TestRequestPayment<HeaderResponse>(new CaseInfo(owner, requester, bucketName_obs, request, 200), 
                logger, Thread.currentThread().getStackTrace()[1].getMethodName()) {
            @Override
            HeaderResponse action(ObsClient obsClient) {
                return obsClient.truncateObject((TruncateObjectRequest) this.getRequest());
            }
        }.doTest();
    }
    
    
}
