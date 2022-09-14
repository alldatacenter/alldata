package com.alibaba.sreworks.other.server.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.alibaba.sreworks.common.util.StringUtil;

import com.google.common.collect.ImmutableMap;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SetBucketPolicyArgs;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MinioService {

    private static final String PUBLIC_BUCKET_NAME = "public";

    private static final String PUBLIC_BUCKET_POLICY = "{\"Version\":\"2012-10-17\","
        + "\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\","
        + "\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"],\"Resource\":[\"arn:aws:s3:::public\"]},"
        + "{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\","
        + "\"s3:ListMultipartUploadParts\",\"s3:PutObject\",\"s3:AbortMultipartUpload\",\"s3:DeleteObject\"],"
        + "\"Resource\":[\"arn:aws:s3:::public/*\"]}]}";

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.accessKey}")
    private String minioAccessKey;

    @Value("${minio.secretKey}")
    private String minioSecretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void postConstruct() throws Exception {
        minioClient = MinioClient.builder()
            .endpoint(minioEndpoint)
            .credentials(minioAccessKey, minioSecretKey)
            .build();
        boolean bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(PUBLIC_BUCKET_NAME).build()
        );
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(PUBLIC_BUCKET_NAME).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(PUBLIC_BUCKET_NAME)
                .config(PUBLIC_BUCKET_POLICY)
                .build());
        }
    }

    public List<Item> listObjects(String parentPath) throws Exception {
        List<Item> ret = new ArrayList<>();
        Iterable<Result<Item>> resultIterable = minioClient.listObjects(ListObjectsArgs.builder()
            .bucket(PUBLIC_BUCKET_NAME)
            .prefix(parentPath)
            .includeUserMetadata(true)
            .build());
        for (Result<Item> itemResult : resultIterable) {
            ret.add(itemResult.get());
        }
        return ret;
    }

    public void putObject(String name, MultipartFile file, Map<String, String> userMetadata) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(PUBLIC_BUCKET_NAME)
            .object(name)
            .stream(file.getInputStream(), file.getSize(), 10485760)
            .contentType(file.getContentType())
            .userMetadata(userMetadata)
            .build());
    }

    public String objectUrl(String name) {
        String uploadSubPath = System.getenv("UPLOAD_SUB_PATH");
        uploadSubPath = StringUtil.isEmpty(uploadSubPath) ? "v0.1" : uploadSubPath;
        String sreworksFilePrefix = System.getenv("SREWORKS_FILE_PREFIX");
        if(name.contains("/")){
            return sreworksFilePrefix + name;
        }else{
            return sreworksFilePrefix + PUBLIC_BUCKET_NAME + "/" + uploadSubPath + "/" + name;
        }
    }

    public String relativeObjectUrl(String name) {
        return PUBLIC_BUCKET_NAME + "/" + name;
    }

    public void removeObject(String name) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(PUBLIC_BUCKET_NAME)
            .object(name)
            .build());
    }

}
