package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.internal.SkipMd5CheckStrategy;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.SSECOSKeyManagementParams;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.utils.Base64;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class KmsUploadDemo {

    public static void main(String[] args) {
        SimpleUploadWithKmsMeta();
        CopyObjectWithKmsMeta();
    }

    public static void SimpleUploadWithKmsMeta() {
        COSCredentials cred = new BasicCOSCredentials("SECRET_ID", "SECRET_KEY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置使用https请求
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";
        File localFile = new File("/test.log");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        String kmsKeyId = "your-kms-key-id";
        String encryptionContext = Base64.encodeAsString("{\"Ssekmstest\":\"Ssekmstest\"}".getBytes());
        SSECOSKeyManagementParams ssecosKeyManagementParams = new SSECOSKeyManagementParams(kmsKeyId, encryptionContext);
        putObjectRequest.setSSECOSKeyManagementParams(ssecosKeyManagementParams);
        // 服务端加密场景下，返回的etag不再代表文件的md5，所以需要去掉客户端的md5校验
        // 如有需要，可获取crc64，自行校验
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        try {
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            // putobjectResult会返回文件的etag
            String etag = putObjectResult.getETag();
            String crc64 = putObjectResult.getCrc64Ecma();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        // 关闭客户端
        cosclient.shutdown();
    }

    public static void MultipartUploadWithKmsMeta() {
        COSCredentials cred = new BasicCOSCredentials("SECRET_ID", "SECRET_KEY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置使用https请求
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";
        String kmsKeyId = "your-kms-key-id";
        String encryptionContext = Base64.encodeAsString("{\"Ssekmstest\":\"Ssekmstest\"}".getBytes());
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
        SSECOSKeyManagementParams ssecosKeyManagementParams = new SSECOSKeyManagementParams(kmsKeyId, encryptionContext);
        // 服务端加密场景下，返回的etag不再代表文件的md5，所以需要去掉客户端的md5校验
        // 如有需要，可获取crc64，自行校验
        System.setProperty(SkipMd5CheckStrategy.DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        initiateMultipartUploadRequest.setSSECOSKeyManagementParams(ssecosKeyManagementParams);
        try {
            InitiateMultipartUploadResult initiateMultipartUploadResult = cosclient.initiateMultipartUpload(initiateMultipartUploadRequest);
            List<PartETag> partETags = new LinkedList<>();
            for (int i = 0; i < 2; i++) {
                byte data[] = new byte[1024 * 1024];
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(initiateMultipartUploadResult.getUploadId());
                // 设置分块的数据来源输入流
                uploadPartRequest.setInputStream(new ByteArrayInputStream(data));
                // 设置分块的长度
                uploadPartRequest.setPartSize(data.length); // 设置数据长度
                uploadPartRequest.setPartNumber(i + 1);     // 假设要上传的part编号是10

                UploadPartResult uploadPartResult = cosclient.uploadPart(uploadPartRequest);
                PartETag partETag = uploadPartResult.getPartETag();
                partETags.add(partETag);
            }
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, initiateMultipartUploadResult.getUploadId(), partETags);
            CompleteMultipartUploadResult completeResult =
                    cosclient.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        // 关闭客户端
        cosclient.shutdown();
    }

    public static void CopyObjectWithKmsMeta() {
        COSCredentials cred = new BasicCOSCredentials("SECRET_ID", "SECRET_KEY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置使用https请求
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid

        String kmsKeyId = "your-kms-key-id";
        String encryptionContext = Base64.encodeAsString("{\"Ssekmstest\":\"Ssekmstest\"}".getBytes());

        // 要拷贝的bucket region, 支持跨园区拷贝
        Region srcBucketRegion = new Region("ap-guangzhou");
        // 源bucket, bucket名需包含appid
        String srcBucketName = "mybucket-1251668577";
        // 要拷贝的源文件
        String srcKey = "aaa/bbb.txt";
        // 目的bucket, bucket名需包含appid
        String destBucketName = "mybucket-1251668577";
        // 要拷贝的目的文件
        String destKey = "ccc/ddd.txt";

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(srcBucketRegion, srcBucketName,
                srcKey, destBucketName, destKey);
        copyObjectRequest.setSSECOSKeyManagementParams(new SSECOSKeyManagementParams(kmsKeyId, encryptionContext));
        try {
            CopyObjectResult copyObjectResult = cosclient.copyObject(copyObjectRequest);
            String crc64 = copyObjectResult.getCrc64Ecma();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        // 关闭客户端
        cosclient.shutdown();
    }
}
