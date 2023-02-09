package com.aliyun.oss.integrationtests;

import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.activation.MimetypesFileTypeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CMKIDTest extends TestBase {

    private String key = "CMKIDTest";

    private String uploadLocalFilePath = "uploadFile";

    private String downloadLocalFilePath = "downloadFile";

    @Test
    public void testPutObjectWithCMKID() {
        try {
            final File sampleFile = createSampleFile(uploadLocalFilePath, 1 * 1024 * 1024);

            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setServerSideEncryption(ObjectMetadata.KMS_SERVER_SIDE_ENCRYPTION);
            metadata.setServerSideEncryptionKeyId(TestConfig.KMS_CMK_ID);
            ossClient.putObject(bucketName, key, sampleFile, metadata);
            ObjectMetadata objectMetadata = ossClient.getObject(new GetObjectRequest(bucketName, key), new File(downloadLocalFilePath));
            Assert.assertEquals(TestConfig.KMS_CMK_ID, objectMetadata.getServerSideEncryptionKeyId());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPostObjectWithCMKID() {
        HttpURLConnection conn = null;

        try {
            final File sampleFile = createSampleFile(uploadLocalFilePath, 1 * 1024 * 1024);

            String urlStr = TestConfig.OSS_TEST_ENDPOINT.replace("http://", "http://" + bucketName + ".");

            Map<String, String> formFields = new LinkedHashMap<String, String>();

            formFields.put("key", this.key);
            formFields.put("Content-Disposition", "attachment;filename="
                    + sampleFile.getAbsolutePath());
            formFields.put("OSSAccessKeyId", TestConfig.OSS_TEST_ACCESS_KEY_ID);
            formFields.put("x-oss-server-side-encryption", ObjectMetadata.KMS_SERVER_SIDE_ENCRYPTION);
            formFields.put("x-oss-server-side-encryption-key-id", TestConfig.KMS_CMK_ID);
            String policy
                    = "{\"expiration\": \"2120-01-01T12:00:00.000Z\",\"conditions\": [[\"content-length-range\", 0, 104857600]]}";
            String encodePolicy = new String(Base64.encodeBase64(policy.getBytes()));
            formFields.put("policy", encodePolicy);
            String signaturecom = computeSignature(TestConfig.OSS_TEST_ACCESS_KEY_SECRET, encodePolicy);
            formFields.put("Signature", signaturecom);

            String boundary = "9431149156168";

            URL url = new URL(urlStr);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);
            OutputStream out = new DataOutputStream(conn.getOutputStream());

            if (formFields != null) {
                StringBuffer strBuf = new StringBuffer();
                Iterator<Map.Entry<String, String>> iter = formFields.entrySet().iterator();
                int i = 0;

                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String inputName = entry.getKey();
                    String inputValue = entry.getValue();

                    if (inputValue == null) {
                        continue;
                    }

                    if (i == 0) {
                        strBuf.append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\""
                                + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    } else {
                        strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\""
                                + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    }

                    i++;
                }
                out.write(strBuf.toString().getBytes());
            }

            String contentType = new MimetypesFileTypeMap().getContentType(sampleFile.getName());
            if (contentType == null || contentType.equals("")) {
                contentType = "application/octet-stream";
            }

            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\r\n").append("--").append(boundary)
                    .append("\r\n");
            strBuf.append("Content-Disposition: form-data; name=\"file\"; "
                    + "filename=\"" + sampleFile.getName() + "\"\r\n");
            strBuf.append("Content-Type: " + contentType + "\r\n\r\n");

            out.write(strBuf.toString().getBytes());

            DataInputStream in = new DataInputStream(new FileInputStream(sampleFile));
            int bytes = 0;
            byte[] bufferOut = new byte[1024];
            while ((bytes = in.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            in.close();

            byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();

            strBuf = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                strBuf.append(line).append("\n");
            }
            System.out.println(strBuf.toString());
            reader.close();

            ObjectMetadata objectMetadata = ossClient.getObject(new GetObjectRequest(bucketName, key), new File(downloadLocalFilePath));
            Assert.assertEquals(TestConfig.KMS_CMK_ID, objectMetadata.getServerSideEncryptionKeyId());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String computeSignature(String accessKeySecret, String encodePolicy)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        // convert to UTF-8
        byte[] key = accessKeySecret.getBytes("UTF-8");
        byte[] data = encodePolicy.getBytes("UTF-8");

        // hmac-sha1
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] sha = mac.doFinal(data);

        // base64
        return new String(Base64.encodeBase64(sha));
    }

    @Test
    public void testInitMultipartUploadWithCMKID() {
        try {
            final File sampleFile = createSampleFile(uploadLocalFilePath, 1 * 1024 * 1024);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setServerSideEncryption(ObjectMetadata.KMS_SERVER_SIDE_ENCRYPTION);
            metadata.setServerSideEncryptionKeyId(TestConfig.KMS_CMK_ID);

            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key, metadata);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            String uploadId = result.getUploadId();
            List<PartETag> partETags = new ArrayList<PartETag>();

            final long partSize = 1 * 1024 * 1024L;
            long fileLength = sampleFile.length();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }

            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                InputStream instream = new FileInputStream(sampleFile);
                instream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }

            Collections.sort(partETags, new Comparator<PartETag>() {
                public int compare(PartETag p1, PartETag p2) {
                    return p1.getPartNumber() - p2.getPartNumber();
                }
            });
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossClient.completeMultipartUpload(completeMultipartUploadRequest);

            ObjectMetadata objectMetadata = ossClient.getObject(new GetObjectRequest(bucketName, key), new File(downloadLocalFilePath));

            Assert.assertEquals(TestConfig.KMS_CMK_ID, objectMetadata.getServerSideEncryptionKeyId());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCopyObjectWithCMKID() {
        try {
            final String sourceBucketName = createBucket();
            final String sourceKey = "src_key";
            final File sampleFile = createSampleFile(uploadLocalFilePath, 1 * 1024 * 1024);
            final CopyObjectRequest request =
                    new CopyObjectRequest(sourceBucketName, sourceKey, bucketName, key);

            ossClient.putObject(sourceBucketName, sourceKey, sampleFile);

            request.setServerSideEncryption(ObjectMetadata.KMS_SERVER_SIDE_ENCRYPTION);
            request.setServerSideEncryptionKeyId(TestConfig.KMS_CMK_ID);
            ossClient.copyObject(request);

            String copyLocalFilePath = "copy";

            ObjectMetadata metadataCopy = ossClient.getObject(new GetObjectRequest(bucketName, key), new File(copyLocalFilePath));

            Assert.assertEquals(TestConfig.KMS_CMK_ID, metadataCopy.getServerSideEncryptionKeyId());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAppendObjectWithCMKID() {
        try {
            String content1 = "Hello OSS A ";
            String content2 = "Hello OSS B ";

            ObjectMetadata meta = new ObjectMetadata();

            meta.setContentType("text/plain");
            meta.setServerSideEncryption(ObjectMetadata.KMS_SERVER_SIDE_ENCRYPTION);
            meta.setServerSideEncryptionKeyId(TestConfig.KMS_CMK_ID);

            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, new ByteArrayInputStream(content1.getBytes()),meta);

            appendObjectRequest.setPosition(0L);

            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);

            appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
            appendObjectRequest.setInputStream(new ByteArrayInputStream(content2.getBytes()));

            ossClient.appendObject(appendObjectRequest);

            OSSObject o = ossClient.getObject(bucketName, key);
            BufferedReader reader = new BufferedReader(new InputStreamReader(o.getObjectContent()));
            StringBuilder sb = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Assert.assertTrue(sb.toString().equals(content1 + content2));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}