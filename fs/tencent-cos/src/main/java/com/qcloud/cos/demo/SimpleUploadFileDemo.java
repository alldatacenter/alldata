package com.qcloud.cos.demo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.region.Region;

/**
 * SimpleUpload 给出了简单上传的示例
 */
public class SimpleUploadFileDemo {
    public static void main(String[] args) {
        SimpleUploadFileFromLocal(false);
    }
    // 将本地文件上传到COS
    public static void SimpleUploadFileFromLocal(boolean useTrafficLimit) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";
        File localFile = new File("src/test/resources/len10M.txt");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        if(useTrafficLimit) {
            // 限流使用的单位是bit/s, 这里测试1MB/s的上传带宽限制
            putObjectRequest.setTrafficLimit(8*1024*1024);
        }
        // 设置存储类型, 默认是标准(Standard), 低频(standard_ia)
        putObjectRequest.setStorageClass(StorageClass.Standard_IA);
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
    
    // 从输入流进行读取并上传到COS
    public static void SimpleUploadFileFromStream() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        
        String key = "aaa/bbb.jpg";
        File localFile = new File("src/test/resources/len10M.txt");
        
        InputStream input = new ByteArrayInputStream(new byte[10]);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        // 从输入流上传必须制定content length, 否则http客户端可能会缓存所有数据，存在内存OOM的情况
        objectMetadata.setContentLength(10);
        // 默认下载时根据cos路径key的后缀返回响应的contenttype, 上传时设置contenttype会覆盖默认值
        objectMetadata.setContentType("image/jpeg");
        
        PutObjectRequest putObjectRequest =
                new PutObjectRequest(bucketName, key, input, objectMetadata);
        // 设置存储类型, 默认是标准(Standard), 低频(standard_ia)
        putObjectRequest.setStorageClass(StorageClass.Standard_IA);
        try {
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            // putobjectResult会返回文件的etag
            String etag = putObjectResult.getETag();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
        
        // 关闭客户端        
        cosclient.shutdown();
    }
}
