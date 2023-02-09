package com.obs.test.tools;

import com.obs.services.ObsClient;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.MultipartUpload;
import com.obs.services.model.MultipartUploadListing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MultipartTools {
    private static final Logger logger = LogManager.getLogger(MultipartTools.class);
    /**
     * 列举所有分段对象
     * @param obsClient
     * @param bucketName
     * @return
     */
    public static List<MultipartUpload> listAllMultipartUpload(ObsClient obsClient, String bucketName) {
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucketName);
        MultipartUploadListing result;

        List<MultipartUpload> multipartUploads = new ArrayList<>();
        do{
            result = obsClient.listMultipartUploads(request);
            for(MultipartUpload upload : result.getMultipartTaskList()){
                multipartUploads.add(upload);
            }
            request.setKeyMarker(result.getNextKeyMarker());
            request.setUploadIdMarker(result.getNextUploadIdMarker());
        }while(result.isTruncated());

        return multipartUploads;
    }

    /**
     * 取消一个分段任务
     * @param obsClient
     * @param bucketName
     * @param objectKey
     * @param uploadId
     */
    public static void abortMultipartUpload(ObsClient obsClient, String bucketName, String objectKey, String uploadId) {
        logger.warn("abort multipart upload : " + bucketName + "   " + uploadId + "   " + objectKey);
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);
        obsClient.abortMultipartUpload(request);
    }

    /**
     * 删除所有分段任务
     * @param obsClient
     * @param bucketName
     */
    public static void abortAllMultipartUpload(ObsClient obsClient, String bucketName) {
        // 列举所有分段对象
        List<MultipartUpload> multipartUploads = listAllMultipartUpload(obsClient, bucketName);
        if(null == multipartUploads) {
            return;
        }

        for(MultipartUpload upload : multipartUploads) {
            abortMultipartUpload(obsClient, bucketName, upload.getObjectKey(), upload.getUploadId());
        }
    }
}
