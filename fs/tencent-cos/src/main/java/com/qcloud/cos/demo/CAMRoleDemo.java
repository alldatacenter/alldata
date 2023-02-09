package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.*;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.region.Region;

import java.io.File;

public class CAMRoleDemo {

    public static void SimpleUploadFileFromLocal() {
        InstanceMetadataCredentialsEndpointProvider endpointProvider =
                new InstanceMetadataCredentialsEndpointProvider(InstanceMetadataCredentialsEndpointProvider.Instance.CVM);

        InstanceCredentialsFetcher instanceCredentialsFetcher = new InstanceCredentialsFetcher(endpointProvider);
        COSCredentialsProvider cosCredentialsProvider = new InstanceCredentialsProvider(instanceCredentialsFetcher);
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        COSClient cosClient = new COSClient(cosCredentialsProvider,clientConfig);
        String bucketName = "3399demo-125xxxxxxxx";
        String key = "test/demo.txt";
        File localFile = new File("test");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        cosClient.shutdown();
    }

    public static void SimpleUploadFileFromEMR() {
        InstanceMetadataCredentialsEndpointProvider endpointProvider =
                new InstanceMetadataCredentialsEndpointProvider(InstanceMetadataCredentialsEndpointProvider.Instance.EMR);

        InstanceCredentialsFetcher instanceCredentialsFetcher = new InstanceCredentialsFetcher(endpointProvider);

        COSCredentialsProvider cosCredentialsProvider = new InstanceCredentialsProvider(instanceCredentialsFetcher);

        COSCredentials cred = cosCredentialsProvider.getCredentials();
        System.out.println(cred.getCOSAccessKeyId());
        System.out.println(cred.getCOSSecretKey());
        System.out.println(cred.getCOSAppId());

        ClientConfig clientConfig = new ClientConfig(new Region("ap-chongqing"));
        COSClient cosClient = new COSClient(cosCredentialsProvider,clientConfig);
        String bucketName = "aaa-125xxx";
        String key = "test_emr.txt";
        File localFile = new File("./test");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

        System.out.println("upload file etag: " + putObjectResult.getETag());
        System.out.println("upload file requestId: " + putObjectResult.getRequestId());

        ObjectMetadata getMeta = cosClient.getObjectMetadata(bucketName, key);
        System.out.println("get file etag: " + getMeta.getETag());

        cosClient.deleteObject(bucketName, key);
        if (cosClient.doesObjectExist(bucketName, key)) {
            System.out.println("delete failed");
        } else {
            System.out.println("delete successfully");
        }

        cosClient.shutdown();
    }

    public static void main(String[] args) {
        SimpleUploadFileFromEMR();
    }
}
