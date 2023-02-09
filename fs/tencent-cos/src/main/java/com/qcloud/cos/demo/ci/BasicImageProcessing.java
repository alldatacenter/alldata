package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GetObjectRequest;

import java.io.File;

public class BasicImageProcessing {
    public static void imageZoomDemo(COSClient cosClient) {
        String bucketName = "examplebucket-1250000000";
        String key = "qrcode.png";
        GetObjectRequest getObj = new GetObjectRequest(bucketName, key);
        // 宽高缩放50%
        String rule = "imageMogr2/thumbnail/!50p";
        getObj.putCustomQueryParameter(rule, null);
        cosClient.getObject(getObj, new File("qrcode-50p.png"));
    }

    public static void imageCroppingDemo(COSClient cosClient) {
        String bucketName = "examplebucket-1250000000";
        String key = "qrcode.png";
        GetObjectRequest getObj = new GetObjectRequest(bucketName, key);
        // 宽高缩放50%
        String rule = "imageMogr2/iradius/150";
        getObj.putCustomQueryParameter(rule, null);
        cosClient.getObject(getObj, new File("qrcode-cropping.png"));
    }

    public static void imageRotateDemo(COSClient cosClient) {
        String bucketName = "examplebucket-1250000000";
        String key = "qrcode.png";
        GetObjectRequest getObj = new GetObjectRequest(bucketName, key);
        // 宽高缩放50%
        String rule = "imageMogr2/rotate/90";
        getObj.putCustomQueryParameter(rule, null);
        cosClient.getObject(getObj, new File("qrcode-rotate.png"));
    }
    public static void main(String[] args) throws Exception {
        COSClient cosClient = ClientUtils.getTestClient();
        imageRotateDemo(cosClient);
        cosClient.shutdown();
    }
}
