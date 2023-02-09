/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ENDPOINT;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@SuppressWarnings("deprecation")
public class TestUtils {

    private static final byte[] ALPHABETS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private static final int MAX_RANDOM_LENGTH = 1 * 1024 * 1024 * 1024; // 1GB

    private static Random rand = new Random();

    private static HttpClient httpClient = null;

    static {
        httpClient = new DefaultHttpClient();
        Scheme sch = new Scheme(Protocol.HTTPS.toString(), 443, getSSLSocketFactory());
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
    }

    private static SSLSocketFactory getSSLSocketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, trustAllCerts, null);
            SSLSocketFactory ssf = new SSLSocketFactory(sslcontext,
                    SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return ssf;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte pickupAlphabet() {
        int idx = new Random().nextInt(ALPHABETS.length);
        return ALPHABETS[idx];
    }

    public static String genRandomString(final int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = pickupAlphabet();
        }
        return new String(data);
    }

    public static String buildObjectKey(String keyPrefix, int seqNum) {
        return keyPrefix + seqNum;
    }

    public static void waitAll(Thread[] targets) {
        for (int i = 0; i < targets.length; i++) {
            targets[i].start();
        }

        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].join();
            } catch (InterruptedException e) {
            }
        }
    }

    public static String genFixedLengthFile(long fixedLength) throws IOException {
        ensureDirExist(TestBase.UPLOAD_DIR);
        String filePath = TestBase.UPLOAD_DIR + System.currentTimeMillis();
        RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
        FileChannel fc = raf.getChannel();

        MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, 0, fixedLength);
        try {
            for (int i = 0; i < fixedLength; i++) {
                mbb.put(pickupAlphabet());
            }
            return filePath;
        } finally {
            if (fc != null) {
                fc.close();
            }

            if (raf != null) {
                raf.close();
            }
        }
    }

    public static String buildFilePath() {
        ensureDirExist(TestBase.DOWNLOAD_DIR);
        return TestBase.DOWNLOAD_DIR + System.currentTimeMillis();
    }

    public static String genRandomLengthFile() throws IOException {
        ensureDirExist(TestBase.UPLOAD_DIR);
        String filePath = TestBase.UPLOAD_DIR + System.currentTimeMillis();
        RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
        FileChannel fc = raf.getChannel();

        long fileLength = rand.nextInt(MAX_RANDOM_LENGTH);
        MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, 0, fileLength);
        try {
            for (int i = 0; i < fileLength; i++) {
                mbb.put(pickupAlphabet());
            }
            return filePath;
        } finally {
            if (fc != null) {
                fc.close();
            }

            if (raf != null) {
                raf.close();
            }
        }
    }

    public static boolean batchPutObject(OSSClient client, String bucketName, List<String> objects) {
        byte[] buf = new byte[1024];
        for (int i = 0; i < buf.length; i++)
            buf[i] = 'a';

        for (String o : objects) {
            try {
                client.putObject(bucketName, o, new ByteArrayInputStream(buf), null);
            } catch (Exception e) {
                // Ignore the exception and return directly.
                return false;
            }
        }
        return true;
    }

    public static InputStream genFixedLengthInputStream(long fixedLength) {
        byte[] buf = new byte[(int) fixedLength];
        for (int i = 0; i < buf.length; i++)
            buf[i] = 'a';
        return new ByteArrayInputStream(buf);
    }

    public static void ensureDirExist(String dir) {
        File f = new File(dir);
        if (!f.exists())
            f.mkdirs();
    }

    public static void removeFiles(List<File> files) {
        for (File f : files) {
            if (f != null && f.exists())
                f.delete();
        }
    }

    public static void removeFile(String filePath) {
        File toRemove = new File(filePath);
        if (toRemove != null && toRemove.exists())
            toRemove.delete();
    }

    public static String claimUploadId(OSSClient client, String bucketName, String key) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);
        Assert.assertEquals(bucketName, result.getBucketName());
        Assert.assertEquals(key, result.getKey());
        return result.getUploadId();
    }

    public static String calcMultipartsETag(List<PartETag> eTags) {
        StringBuffer concatedEtags = new StringBuffer();
        for (PartETag e : eTags) {
            concatedEtags.append(e.getETag());
        }

        String md5Digest = BinaryUtil.encodeMD5(concatedEtags.toString().getBytes());
        int partNumber = eTags.size();
        String finalETag = String.format("%s-%d", md5Digest, partNumber);
        return finalETag;
    }

    public static String composeLocation(String endpoint, String bucketName, String key) {
        try {
            URI baseUri = URI.create(endpoint);
            URI resultUri = new URI(baseUri.getScheme(),
                    null,
                    bucketName + "." + baseUri.getHost(),
                    baseUri.getPort(),
                    String.format("/%s", HttpUtil.urlEncode(key, DEFAULT_CHARSET_NAME)),
                    null,
                    null);
            return URLDecoder.decode(resultUri.toString(), DEFAULT_CHARSET_NAME);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static String composeLocation(OSSClient client, String endpoint, String bucketName, String key) {
        try {
            URI baseUri = URI.create(endpoint);
            URI resultUri = null;
            if (client.getClientConfiguration().isSLDEnabled()) {
                resultUri = new URI(baseUri.getScheme(),
                        null,
                        baseUri.getHost(),
                        baseUri.getPort(),
                        String.format("/%s/%s", bucketName, HttpUtil.urlEncode(key, DEFAULT_CHARSET_NAME)),
                        null,
                        null);
            } else {
                resultUri = new URI(baseUri.getScheme(),
                        null,
                        bucketName + "." + baseUri.getHost(),
                        baseUri.getPort(),
                        String.format("/%s", HttpUtil.urlEncode(key, DEFAULT_CHARSET_NAME)),
                        null,
                        null);
            }

            return URLDecoder.decode(resultUri.toString(), DEFAULT_CHARSET_NAME);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static void waitForCacheExpiration(int durationSeconds) {
        try {
            Thread.sleep(durationSeconds * 1000);
        } catch (InterruptedException e) {
        }
    }

    static class StsToken {
        private String accessKeyId;
        private String secretAccessKey;
        private String securityToken;

        public StsToken(String accessKeyId, String secretAccessKey, String securityToken) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.securityToken = securityToken;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getSecurityToken() {
            return securityToken;
        }

        public void setSecurityToken(String securityToken) {
            this.securityToken = securityToken;
        }
    }

    public static OSSClient createSessionClient(List<String> actions, List<String> resources) throws JSONException {
        String tokenPolicy = jsonizeTokenPolicy(actions, resources, true);
        StsToken token = getStsToken("", "", 3600, tokenPolicy);
        return new OSSClient(OSS_TEST_ENDPOINT, token.accessKeyId, token.secretAccessKey, token.securityToken,
                new ClientConfiguration().setSupportCname(false));
    }

    public static String jsonizeTokenPolicy(List<String> actions, List<String> resources, boolean allow) throws JSONException {
        JSONObject stmtJsonObject = new JSONObject();
        stmtJsonObject.put("Action", actions);
        stmtJsonObject.put("Resource", resources);
        stmtJsonObject.put("Effect", allow ? "Allow" : "Deny");

        JSONArray stmtJsonArray = new JSONArray();
        stmtJsonArray.put(0, stmtJsonObject);

        JSONObject policyJsonObject = new JSONObject();
        policyJsonObject.put("Version", "1");
        policyJsonObject.put("Statement", stmtJsonArray);

        return policyJsonObject.toString();
    }

    public static StsToken getStsToken(String grantor, String grantee,
                                       long durationSeconds, String policy) {
        try {
            URI apiUri = new URI(Protocol.HTTPS.toString(), null, "", 80,
                    "", null, null);

            HttpPost httpPost = new HttpPost(apiUri);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("STSVERSION", "1"));
            nvps.add(new BasicNameValuePair("CALLERUID", grantor));
            nvps.add(new BasicNameValuePair("GRANTEE", grantee));
            nvps.add(new BasicNameValuePair("DURATIONSECONDS", String.valueOf(durationSeconds)));
            nvps.add(new BasicNameValuePair("POLICY", policy));
            nvps.add(new BasicNameValuePair("APIUSERNAME", ""));
            nvps.add(new BasicNameValuePair("APIPASSWORD", ""));
            nvps.add(new BasicNameValuePair("MFAPresent", "false"));
            nvps.add(new BasicNameValuePair("OwnerId", grantor));
            nvps.add(new BasicNameValuePair("CallerType", "customer"));
            nvps.add(new BasicNameValuePair("ProxyTrustTransportInfo", "false"));
            nvps.add(new BasicNameValuePair("CallerIp", "127.0.0.1"));
            nvps.add(new BasicNameValuePair("ProxyCallerIp", "127.0.0.1"));
            nvps.add(new BasicNameValuePair("ProxyCallerSecurityTransport", "false"));
            nvps.add(new BasicNameValuePair("callerSecurityTransport", "true"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            InputStream responseBody = httpResponse.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody));
            String line = null;
            StringBuilder tokenString = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                tokenString.append(line);
                System.out.println(line);
            }

            JSONObject tokenJsonObject = new JSONObject(tokenString.toString());
            JSONObject credsJsonObjson = tokenJsonObject.getJSONObject("Credentials");
            String accessKeyId = credsJsonObjson.getString("AccessKeyId");
            String secretAccessKey = credsJsonObjson.getString("AccessKeySecret");
            String securityToken = credsJsonObjson.getString("SecurityToken");

            return new StsToken(accessKeyId, secretAccessKey, securityToken);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static byte[] InputStream2ByteArray(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        byte[] data = toByteArray(in);
        in.close();

        return data;
    }

    private static byte[] toByteArray(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    public static String getHttpsEndpoint(String endpoint) {
        if (endpoint.startsWith("http://")) {
            return endpoint.replace("http://", "https://");
        }else if (!endpoint.startsWith("http")) {
            return "https://" + endpoint;
        }
        return endpoint;
    }
}