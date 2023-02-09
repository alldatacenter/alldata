package com.qcloud.cos.demo;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.region.Region;


/**
 * 分开上传demo展示了初始化分块，分快上传拷贝, 列出已上传分块，完成分块，终止分块的demo
 * 分块上传的完成逻辑较复杂，需要经历多个步骤, 建议用户使用TransferManager中封装好的上传接口来进行文件的上传
 */
public class MultipartUploadDemo {
    public static String InitMultipartUploadDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
        // 设置存储类型, 默认是标准(Standard), 低频(Standard_IA), 归档(Archive)
        request.setStorageClass(StorageClass.Standard);
        try {
            InitiateMultipartUploadResult initResult = cosclient.initiateMultipartUpload(request);
            // 获取uploadid
            String uploadId = initResult.getUploadId();
            return uploadId;
        } catch (CosServiceException e) {
            throw e;
        } catch (CosClientException e) {
            throw e;
        } finally {
            cosclient.shutdown();
        }
    }

    // list part用于获取已上传的分片, 如果已上传的分片数量较多, 需要循环多次调用list part获取已上传的所有的分片
    public static List<PartETag> listPartDemo(String uploadId) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        // uploadid(通过initiateMultipartUpload或者ListMultipartUploads获取)

        List<PartETag> partETags = new LinkedList<>();      // 用于保存已上传的分片信息
        PartListing partListing = null;
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
        do {
            try {
                partListing = cosclient.listParts(listPartsRequest);
            } catch (CosServiceException e) {
                throw e;
            } catch (CosClientException e) {
                throw e;
            }
            for (PartSummary partSummary : partListing.getParts()) {
                partETags.add(new PartETag(partSummary.getPartNumber(), partSummary.getETag()));
            }
            listPartsRequest.setPartNumberMarker(partListing.getNextPartNumberMarker());
        } while (partListing.isTruncated());

        cosclient.shutdown();
        return partETags;
    }

    // 分块上传(上传某一个分片的数据)
    public static List<PartETag> UploadPartDemo(String uploadId) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        // uploadid(通过initiateMultipartUpload或者ListMultipartUploads获取)

        boolean userTrafficLimit = false;
        List<PartETag> partETags = new LinkedList<>();

        // 生成要上传的数据, 这里初始化一个10M的数据
        for (int i = 0; i < 10; i++) {
            byte data[] = new byte[1024 * 1024];
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setUploadId(uploadId);
            // 设置分块的数据来源输入流
            uploadPartRequest.setInputStream(new ByteArrayInputStream(data));
            // 设置分块的长度
            uploadPartRequest.setPartSize(data.length); // 设置数据长度
            uploadPartRequest.setPartNumber(i+1);     // 假设要上传的part编号是10
            if(userTrafficLimit) {
                uploadPartRequest.setTrafficLimit(8*1024*1024);
            }

            try {
                UploadPartResult uploadPartResult = cosclient.uploadPart(uploadPartRequest);
                PartETag partETag = uploadPartResult.getPartETag();
                partETags.add(partETag);
                String crc64 = uploadPartResult.getCrc64Ecma();
            } catch (CosServiceException e) {
                throw e;
            } catch (CosClientException e) {
                throw e;
            }
        }
        cosclient.shutdown();
        return partETags;
    }

    // complete完成分片上传
    public static void completePartDemo(String uploadId, List<PartETag> partETags) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        // uploadid(通过initiateMultipartUpload或者ListMultipartUploads获取)


        // 分片上传结束后，调用complete完成分片上传
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
        try {
            CompleteMultipartUploadResult completeResult =
                    cosclient.completeMultipartUpload(completeMultipartUploadRequest);
            String etag = completeResult.getETag();
            String crc64 = completeResult.getCrc64Ecma();
        } catch (CosServiceException e) {
            throw e;
        } catch (CosClientException e) {
            throw e;
        }

        cosclient.shutdown();
    }

    // 终止分块上传
    public static void abortPartUploadDemo(String uploadId) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        // uploadid(通过initiateMultipartUpload或者ListMultipartUploads获取)

        AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucketName, key, uploadId);
        try {
            cosclient.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        cosclient.shutdown();
    }

    // 分块copy, 表示该块的数据来自另外一个文件的某一范围, 支持跨园区, 跨bucket copy
    public static void copyPartUploadDemo(String uploadId) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";

        CopyPartRequest copyPartRequest = new CopyPartRequest();
        // 要拷贝的源文件所在的region
        copyPartRequest.setSourceBucketRegion(new Region("ap-guangzhou"));
        // 要拷贝的源文件的bucket名称
        copyPartRequest.setSourceBucketName(bucketName);
        // 要拷贝的源文件的路径
        copyPartRequest.setSourceKey("aaa/ccc.txt");
        // 指定要拷贝的源文件的数据范围(类似content-range)
        copyPartRequest.setFirstByte(0L);
        copyPartRequest.setLastByte(1048575L);
        // 目的bucket名称
        copyPartRequest.setDestinationBucketName(bucketName);
        // 目的路径名称
        copyPartRequest.setDestinationKey(key);
        copyPartRequest.setPartNumber(1);
        // uploadId
        copyPartRequest.setUploadId(uploadId);
        try {
            CopyPartResult copyPartResult = cosclient.copyPart(copyPartRequest);
            PartETag partETag = copyPartResult.getPartETag();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        cosclient.shutdown();
    }

    public static void main(String[] args) {
        String uploadId = InitMultipartUploadDemo();
        List<PartETag> partETags = UploadPartDemo(uploadId);
        completePartDemo(uploadId, partETags);
    }
}
