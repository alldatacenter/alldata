package com.qcloud.cos.demo;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.CopyResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Copy;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.utils.Md5Utils;


public class CopyFileDemo {
    // copyObject最大支持5G文件的copy, 5G以上的文件copy请参照TransferManagerDemo中的copy示例
    public static void copySmallFileDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // 要拷贝的bucket region, 支持跨园区拷贝
        Region srcBucketRegion = new Region("ap-shanghai");
        // 源bucket, bucket名需包含appid
        String srcBucketName = "srcBucket-1251668577";
        // 要拷贝的源文件
        String srcKey = "aaa/bbb.txt";
        // 目的bucket, bucket名需包含appid
        String destBucketName = "destBucket-1251668577";
        // 要拷贝的目的文件
        String destKey = "ccc/ddd.txt";

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(srcBucketRegion, srcBucketName,
                srcKey, destBucketName, destKey);
        try {
            CopyObjectResult copyObjectResult = cosclient.copyObject(copyObjectRequest);
            String crc64 = copyObjectResult.getCrc64Ecma();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
        cosclient.shutdown();
    }

    // 对于5G以上的文件，需要通过分块上传中的copypart来实现，步骤较多,实现较复杂。
    // 因此在TransferManager中封装了一个copy接口，能根据文件大小自动的选择接口，既支持5G以下的文件copy, 也支持5G以上的文件copy。推荐使用该接口进行文件的copy。
    public static void copyBigFileDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);


        ExecutorService threadPool = Executors.newFixedThreadPool(32);
        // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池。
        TransferManager transferManager = new TransferManager(cosclient, threadPool);

        // 要拷贝的bucket region, 支持跨园区拷贝
        Region srcBucketRegion = new Region("ap-shanghai");
        // 源bucket, bucket名需包含appid
        String srcBucketName = "srcBucket-1251668577";
        // 要拷贝的源文件
        String srcKey = "aaa/bbb.txt";
        // 目的bucket, bucket名需包含appid
        String destBucketName = "destBucket-1251668577";
        // 要拷贝的目的文件
        String destKey = "ccc/ddd.txt";

        // 生成用于获取源文件信息的srcCOSClient
        COSClient srcCOSClient = new COSClient(cred, new ClientConfig(srcBucketRegion));
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(srcBucketRegion, srcBucketName,
                srcKey, destBucketName, destKey);
        try {
            Copy copy = transferManager.copy(copyObjectRequest, srcCOSClient, null);
            // 返回一个异步结果copy, 可同步的调用waitForCopyResult等待copy结束, 成功返回CopyResult, 失败抛出异常.
            CopyResult copyResult = copy.waitForCopyResult();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        transferManager.shutdownNow();
        srcCOSClient.shutdown();
        cosclient.shutdown();
    }

    public static void copyWithNewMetaDataDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        ExecutorService threadPool = Executors.newFixedThreadPool(32);
        // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池。
        TransferManager transferManager = new TransferManager(cosclient, threadPool);

        // 要拷贝的bucket region, 支持跨园区拷贝
        Region srcBucketRegion = new Region("ap-shanghai");
        // 源bucket, bucket名需包含appid
        String srcBucketName = "srcBucket-1251668577";
        // 要拷贝的源文件
        String srcKey = "aaa/bbb.txt";
        // 目的bucket, bucket名需包含appid
        String destBucketName = "destBucket-1251668577";
        // 要拷贝的目的文件
        String destKey = "ccc/ddd.txt";

        ClientConfig srcClientConfig = new ClientConfig(srcBucketRegion);
        COSClient srcCosclient = new COSClient(cred, srcClientConfig);
        GetObjectRequest getReq = new GetObjectRequest(srcBucketName, srcKey);

        File srcFile = new File("srcFile");

        srcCosclient.getObject(getReq, srcFile);

        String srcMD5 = "";
        try {
            srcMD5 = Md5Utils.md5Hex(srcFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ObjectMetadata destMeta = new ObjectMetadata();
        destMeta.addUserMetadata("md5", srcMD5);

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(srcBucketRegion, srcBucketName,
                srcKey, destBucketName, destKey);
        copyObjectRequest.setNewObjectMetadata(destMeta);

        try {
            CopyObjectResult copyObjectResult = cosclient.copyObject(copyObjectRequest);
            System.out.print(copyObjectResult.getRequestId());
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
        cosclient.shutdown();
    }

    public static void main(String[] args) {
        copyWithNewMetaDataDemo();
    }
}
