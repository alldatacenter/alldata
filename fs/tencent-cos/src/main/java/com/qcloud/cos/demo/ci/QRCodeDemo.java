package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;


public class QRCodeDemo {
    public static void identifyQrCode(COSClient cosClient) {
        // bucket名需包含appid
        // api 请参考 https://cloud.tencent.com/document/product/436/54070
        String bucketName = "examplebucket-1250000000";

        String key = "qrcode.png";
        File localFile = new File("E://qrcode.png");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule1 = new PicOperations.Rule();
        rule1.setBucket(bucketName);
        rule1.setFileId("qrcode-1.png");
        rule1.setRule("QRcode/cover/1");
        ruleList.add(rule1);
        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            CIUploadResult ciUploadResult = putObjectResult.getCiUploadResult();
            System.out.println(putObjectResult.getRequestId());
            System.out.println(ciUploadResult.getOriginalInfo().getEtag());
            for(CIObject ciObject:ciUploadResult.getProcessResults().getObjectList()) {
                System.out.println(ciObject.getLocation());
            }
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
    }

    public static void identifyQrCodeWithMultipart(COSClient cosClient) throws FileNotFoundException {
        // bucket名需包含appid
        // api 请参考 https://cloud.tencent.com/document/product/436/54070
        String bucketName = "examplebucket-1250000000";

        String key = "qrcode.png";
        File localFile = new File("E://qrcode.png");
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
        InitiateMultipartUploadResult initResult = cosClient.initiateMultipartUpload(request);
        String uploadId = initResult.getUploadId();

        // 上传分块
        List<PartETag> partETags = new LinkedList<>();
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(bucketName);
        uploadPartRequest.setKey(key);
        uploadPartRequest.setUploadId(uploadId);
        // 设置分块的数据来源输入流
        uploadPartRequest.setInputStream(new FileInputStream(localFile));
        // 设置分块的长度
        uploadPartRequest.setPartSize(localFile.length()); // 设置数据长度
        uploadPartRequest.setPartNumber(1);     // 假设要上传的part编号是10
        UploadPartResult uploadPartResult = cosClient.uploadPart(uploadPartRequest);
        PartETag partETag = uploadPartResult.getPartETag();
        partETags.add(partETag);

        // 带上图像处理参数执行合并分块
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule1 = new PicOperations.Rule();
        rule1.setBucket(bucketName);
        rule1.setFileId("qrcode-1.png");
        rule1.setRule("QRcode/cover/1");
        ruleList.add(rule1);
        picOperations.setRules(ruleList);

        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
        completeMultipartUploadRequest.setPicOperations(picOperations);
        CompleteMultipartUploadResult completeMultipartUploadResult =
                cosClient.completeMultipartUpload(completeMultipartUploadRequest);

        // 获取图片处理的结果
        CIUploadResult ciUploadResult = completeMultipartUploadResult.getCiUploadResult();
        System.out.println(completeMultipartUploadResult.getRequestId());
        System.out.println(ciUploadResult.getOriginalInfo().getEtag());
        for(CIObject ciObject:ciUploadResult.getProcessResults().getObjectList()) {
            System.out.println(ciObject.getLocation());
        }

    }

    public static void identifyQrCodeWithTransferManager(TransferManager transferManager) throws InterruptedException {
        // bucket名需包含appid
        // api 请参考 https://cloud.tencent.com/document/product/436/54070
        String bucketName = "examplebucket-1250000000";

        String key = "qrcode.png";
        File localFile = new File("E://qrcode.png");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule rule1 = new PicOperations.Rule();
        rule1.setBucket(bucketName);
        rule1.setFileId("qrcode-1.png");
        rule1.setRule("QRcode/cover/1");
        ruleList.add(rule1);
        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);
        Upload upload = transferManager.upload(putObjectRequest);
        UploadResult uploadResult = upload.waitForUploadResult();
        CIUploadResult ciUploadResult = uploadResult.getCiUploadResult();
        System.out.println(uploadResult.getRequestId());
        System.out.println(ciUploadResult.getOriginalInfo().getEtag());
        for(CIObject ciObject:ciUploadResult.getProcessResults().getObjectList()) {
            System.out.println(ciObject.getLocation());
        }
    }
    public static void main(String[] args) throws Exception {
        COSClient cosClient = ClientUtils.getTestClient();
        // 小于5GB文件用简单上传
        identifyQrCode(cosClient);
        cosClient.shutdown();
    }
}
