package com.qcloud.cos.demo;

import com.qcloud.cos.auth.COSSigner;
import com.qcloud.cos.utils.VersionInfoUtils;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostObjectDemo {
    public static void main(String[] args) throws Exception {
        PostObjectUploadDemo();
    }

    private static void PostObjectUploadDemo() throws Exception {
        String bucketName = "mybucket-1251668577";
        String endpoint = "cos.ap-guangzhou.myqcloud.com";
        String key = "images/test.jpg";
        String filename = "test.jpg";
        String inputFilePath = "test.jpg";
        String contentType = "image/jpeg";
        String secretId = "AKIDXXXXXXXX";
        String seretKey = "1A2Z3YYYYYYYYYY";
        long startTimestamp = System.currentTimeMillis() / 1000;
        long endTimestamp = startTimestamp +  30 * 60;
        String endTimestampStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").
                format(endTimestamp * 1000);
        String keyTime = startTimestamp + ";" + endTimestamp;
        String boundary = "----WebKitFormBoundaryZBPbaoYE2gqeB21N";
        // 设置表单的body字段值
        Map<String, String> formFields = new HashMap<>();
        formFields.put("q-sign-algorithm", "sha1");
        formFields.put("key", key);
        formFields.put("q-ak", secretId);
        formFields.put("q-key-time", keyTime);
        // 构造policy，参考文档: https://cloud.tencent.com/document/product/436/14690
        String policy = "{\n" +
                "    \"expiration\": \"" + endTimestampStr + "\",\n" +
                "    \"conditions\": [\n" +
                "        { \"bucket\": \"" + bucketName + "\" },\n" +
                "        { \"q-sign-algorithm\": \"sha1\" },\n" +
                "        { \"q-ak\": \"" + secretId + "\" },\n" +
                "        { \"q-sign-time\":\"" + keyTime + "\" }\n" +
                "    ]\n" +
                "}";
        // policy需要base64后算放入表单中
        String encodedPolicy = new String(Base64.encodeBase64(policy.getBytes()));
        // 设置policy
        formFields.put("policy", encodedPolicy);
        // 根据编码后的policy和secretKey计算签名
        COSSigner cosSigner = new COSSigner();
        String signature = cosSigner.buildPostObjectSignature(seretKey,
                keyTime, policy);
        // 设置签名
        formFields.put("q-signature", signature);
        // 根据以上表单参数，构造最开始的body部分
        String formBody = buildPostObjectBody(boundary, formFields,
                filename, contentType);
        HttpURLConnection conn = null;
        try {
            String urlStr = "http://" + bucketName + "." + endpoint;
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", VersionInfoUtils.getUserAgent());
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream out = new DataOutputStream(conn.getOutputStream());
            // 写入表单的最开始部分
            out.write(formBody.getBytes());
            // 将文件内容写入到输出流中
            File file = new File(inputFilePath);
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            int readBytes;
            byte[] bytes = new byte[4096];
            while ((readBytes = in.read(bytes)) != -1) {
                out.write(bytes, 0, readBytes);
            }
            in.close();
            // 添加最后一个分割符，行首和行尾都是--
            byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();
            // 读取响应头部
            for (Map.Entry<String, List<String>> entries : conn.getHeaderFields().entrySet()) {
                String values = "";
                for (String value : entries.getValue()) {
                    values += value + ",";
                }
                if(entries.getKey() == null) {
                    System.out.println("reponse line:" +  values );
                } else {
                    System.out.println(entries.getKey() + ":" +  values );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String buildPostObjectBody(String boundary, Map<String, String> formFields,
                                             String filename, String contentType) {
        StringBuffer stringBuffer = new StringBuffer();
        for(Map.Entry entry: formFields.entrySet()) {
            // 添加boundary行,行首以--开头
            stringBuffer.append("--").append(boundary).append("\r\n");
            // 字段名
            stringBuffer.append("Content-Disposition: form-data; name=\""
                    + entry.getKey() + "\"\r\n\r\n");
            // 字段值
            stringBuffer.append(entry.getValue() + "\r\n");
        }
        // 添加boundary行,行首以--开头
        stringBuffer.append("--").append(boundary).append("\r\n");
        // 文件名
        stringBuffer.append("Content-Disposition: form-data; name=\"file\"; "
                + "filename=\"" + filename + "\"\r\n");
        // 文件类型
        stringBuffer.append("Content-Type: " + contentType + "\r\n\r\n");
        return stringBuffer.toString();
    }
}
