package com.qcloud.cos.demo;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.COSEncryptionClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoMode;
import com.qcloud.cos.internal.crypto.CryptoStorageMode;
import com.qcloud.cos.internal.crypto.KMSEncryptionMaterials;
import com.qcloud.cos.internal.crypto.KMSEncryptionMaterialsProvider;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import com.qcloud.cos.transfer.Upload;

public class KMSEncryptionClientDemo {
	static String cmk = "kms-xxxxxxx";

	static String bucketName = "mybucket-1251668577";
	static String key = "testKMS/len1m.txt";
	static File localFile = new File("len1m.txt");
    static File bigLocalFile = new File("len10m.txt");

	static COSClient cosClient = createCosClient();

	static COSClient createCosClient() {
		return createCosClient("ap-guangzhou");
	}

	static COSClient createCosClient(String region) {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 为防止请求头部被篡改导致的数据无法解密，强烈建议只使用 https 协议发起请求
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 初始化 KMS 加密材料
        KMSEncryptionMaterials encryptionMaterials = new KMSEncryptionMaterials(cmk);
        // 使用AES/CTR模式，并将加密信息存储在文件元信息中.
        // 如果想要此次加密的对象被 COS 其他的 SDK 解密下载，则必须选择 AesCtrEncryption 模式
        CryptoConfiguration cryptoConf = new CryptoConfiguration(CryptoMode.AesCtrEncryption)
                .withStorageMode(CryptoStorageMode.ObjectMetadata);

        //// 如果 kms 服务的 region 与 cos 的 region 不一致，则在加密信息里指定 kms 服务的 region
        //cryptoConf.setKmsRegion(kmsRegion);

        //// 如果需要可以为 KMS 服务的 cmk 设置对应的描述信息。
        //encryptionMaterials.addDescription("kms-region", "guangzhou");

        // 生成加密客户端EncryptionClient, COSEncryptionClient是COSClient的子类, 所有COSClient支持的接口他都支持。
        // EncryptionClient覆盖了COSClient上传下载逻辑，操作内部会执行加密操作，其他操作执行逻辑和COSClient一致
        COSEncryptionClient cosEncryptionClient =
                new COSEncryptionClient(new COSStaticCredentialsProvider(cred),
                        new KMSEncryptionMaterialsProvider(encryptionMaterials), clientConfig,
                        cryptoConf);

		return cosEncryptionClient;
	}

    static void getObjectDemo() {
        // 下载文件
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        File downloadFile = new File("downlen1m.txt");
        ObjectMetadata objectMetadata = cosClient.getObject(getObjectRequest, downloadFile);
		System.out.println(objectMetadata.getRequestId());
    }

	static void putObjectDemo() {
        // 上传文件
        // 这里给出putObject的示例, 对于高级API上传，只用在生成TransferManager时传入COSEncryptionClient对象即可
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
		PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
		System.out.println(putObjectResult.getRequestId());
	}

	static void deleteObjectDemo() {
        // 删除文件
		cosClient.deleteObject(bucketName, key);
	}

    static void transferManagerDemo() {
        ExecutorService threadPool = Executors.newFixedThreadPool(32);
        // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池。
        TransferManager transferManager = new TransferManager(cosClient, threadPool);
        TransferManagerConfiguration transferManagerConfiguration = new TransferManagerConfiguration();
        transferManagerConfiguration.setMultipartUploadThreshold(1024 * 1024);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, bigLocalFile);
        try {
            // 返回一个异步结果Upload, 可同步的调用waitForUploadResult等待upload结束, 成功返回UploadResult, 失败抛出异常.
            Upload upload = transferManager.upload(putObjectRequest);
            UploadResult uploadResult = upload.waitForUploadResult();
            System.out.println(uploadResult.getRequestId());
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        try {
            // 返回一个异步结果Upload, 可同步的调用waitForUploadResult等待download结束, 成功返回DownloadResult, 失败抛出异常.
            Download download = transferManager.download(getObjectRequest, new File("downLen10m.txt"));
            download.waitForCompletion();
            System.out.println(download.getObjectMetadata().getRequestId());
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        transferManager.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
		putObjectDemo();
		getObjectDemo();
        transferManagerDemo();
        // 关闭
        cosClient.shutdown();
    }
}
