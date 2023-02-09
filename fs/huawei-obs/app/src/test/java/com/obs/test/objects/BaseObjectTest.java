package com.obs.test.objects;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.PartEtag;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.UploadPartRequest;

import com.obs.test.UserInfo;
import com.obs.test.buckets.BaseBucketTest;

public abstract class BaseObjectTest extends BaseBucketTest {
    protected byte[] getByte(int size) {
        return getByte(size, (byte) 0);
    }

    protected byte[] getByte(int size, byte defaultValue) {
        byte[] v = new byte[size];
        for (int i = 0; i < size; i++) {
            v[i] = defaultValue;
        }
        return v;
    }

    /**
     * 使用指定的用户，生产一个对象
     * 
     * @param oper
     * @param bucketName
     * @param objectKey
     * @return
     */
    protected PutObjectResult generateTestObject(UserInfo oper, String bucketName, String objectKey,
            boolean isRequesterPays, int size) {
        PutObjectRequest putRequest = generatePutObjectRequest(bucketName, objectKey, size);
        putRequest.setRequesterPays(isRequesterPays);
        return oper.getObsClient().putObject(putRequest);
    }

    /**
     * 生成一个上传对象请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected PutObjectRequest generatePutObjectRequest(String bucketName, String objectKey, int size) {
        PutObjectRequest request = new PutObjectRequest();
        request.setBucketName(bucketName);

        request.setInput(new ByteArrayInputStream(getByte(size)));

        request.setObjectKey(objectKey);

        return request;
    }

    /**
     * 生成复制对象请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected CopyObjectRequest generateCopyObjectRequest(String bucketName, String objectKey) {
        CopyObjectRequest request = new CopyObjectRequest(bucketName, objectKey, bucketName, objectKey + "_copy_dest");
        request.setBucketName(bucketName);

        request.setReplaceMetadata(true);

        return request;
    }

    /**
     * 生成初始化分段上次请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected InitiateMultipartUploadRequest generateInitiateMultipartUploadRequest(String bucketName,
            String objectKey) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        return request;
    }

    /**
     * 生成列举分段请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected ListPartsRequest generateListPartsRequest(String bucketName, String objectKey, String uploadId) {
        ListPartsRequest request = new ListPartsRequest(bucketName, objectKey, uploadId);
        return request;
    }

    /**
     * 生成上传分段的请求
     * 
     * @param bucketName
     * @param objectKey
     * @param uploadId
     * @param partNumber
     * @return
     * @throws IOException
     */
    protected UploadPartRequest generateUploadPartRequest(String bucketName, String objectKey, String uploadId,
            int partNumber) throws IOException {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(uploadId);
        request.setPartNumber(partNumber);

        request.setInput(new ByteArrayInputStream(getByte(6 * 1024 * 1024)));

        return request;
    }
    
    /**
     * 生成上传分段的请求
     * 
     * @param bucketName
     * @param objectKey
     * @param uploadId
     * @param partNumber
     * @return
     * @throws IOException
     */
    protected UploadPartRequest generateUploadPartRequest(String bucketName, String objectKey, String uploadId,
            int partNumber, byte defaultValue) throws IOException {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(uploadId);
        request.setPartNumber(partNumber);

        request.setInput(new ByteArrayInputStream(getByte(6 * 1024 * 1024, defaultValue)));

        return request;
    }

    private File createSampleFile() throws IOException {
        File file = File.createTempFile("obs-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        for (int i = 0; i < 1000000; i++) {
            writer.write(UUID.randomUUID() + "\n");
            writer.write(UUID.randomUUID() + "\n");
        }
        writer.flush();
        writer.close();

        return file;
    }

    /**
     * 生成取消分段请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected AbortMultipartUploadRequest generateAbortMultipartUploadRequest(String bucketName, String objectKey,
            String uploadId) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);
        return request;
    }

    /**
     * 生成合并分段请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected CompleteMultipartUploadRequest generateCompleteMultipartUploadRequest(ListPartsResult listResult) {
        List<PartEtag> partEtags = new ArrayList<PartEtag>();
        for (Multipart m : listResult.getMultipartList()) {
            PartEtag part1 = new PartEtag();
            part1.setPartNumber(m.getPartNumber());
            part1.setEtag(m.getEtag());
            partEtags.add(part1);
        }

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(listResult.getBucket(),
                listResult.getKey(), listResult.getUploadId(), partEtags);
        return request;
    }

    /**
     * 生成列举分段任务请求
     * 
     * @param bucketName
     * @param objectKey
     * @return
     * @throws UnsupportedEncodingException
     */
    protected ListMultipartUploadsRequest generateListMultipartUploadsRequest(String bucketName) {
        return new ListMultipartUploadsRequest(bucketName);
    }
}
