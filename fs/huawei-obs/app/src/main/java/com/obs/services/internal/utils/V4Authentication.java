/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.security.BasicSecurityKey;
import com.obs.services.internal.security.ProviderCredentials;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class V4Authentication {

    public static final String CONTENT_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private String ak;

    private String sk;

    private String region;

    private String nowISOtime;

    protected V4Authentication() {
    }

    public String getAk() {
        return ak;
    }

    public void setAk(String ak) {
        this.ak = ak;
    }

    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public static String caculateSignature(String stringToSign, String shortDate, String sk) throws Exception {
        byte[] dateKey = V4Authentication.hmacSha256Encode(("AWS4" + sk).getBytes(StandardCharsets.UTF_8),
                shortDate);
        byte[] dataRegionKey = V4Authentication.hmacSha256Encode(dateKey, ObsConstraint.DEFAULT_BUCKET_LOCATION_VALUE);

        byte[] dateRegionServiceKey = V4Authentication.hmacSha256Encode(dataRegionKey, Constants.SERVICE);

        byte[] signingKey = V4Authentication.hmacSha256Encode(dateRegionServiceKey, Constants.REQUEST_TAG);

        return V4Authentication.byteToHex(V4Authentication.hmacSha256Encode(signingKey, stringToSign));
    }

    public static IAuthentication makeServiceCanonicalString(String method, Map<String, String> headers,
            String strURIPath, ProviderCredentials credent, Date date, BasicSecurityKey securityKey)
                    throws ServiceException {
        V4Authentication v4 = new V4Authentication();
        v4.setAk(securityKey.getAccessKey());
        v4.setSk(securityKey.getSecretKey());
        v4.setRegion(credent.getRegion());
        v4.setNowISOTime(date);

        List<String> signedAndCanonicalList = v4.getSignedAndCanonicalHeaders(headers);

        String scope = v4.getScope();
        try {
            String canonicalRequest = v4.getCanonicalRequest(method, strURIPath, signedAndCanonicalList);
            String stringToSign = new StringBuilder(Constants.V4_ALGORITHM).append("\n").append(v4.nowISOtime)
                    .append("\n").append(scope).append("\n")
                    .append(V4Authentication.byteToHex(V4Authentication.sha256encode(canonicalRequest))).toString();
            String signature = V4Authentication
                    .byteToHex(V4Authentication.hmacSha256Encode(v4.getSigningKey(), stringToSign));
            String auth = new StringBuilder(Constants.V4_ALGORITHM).append(" Credential=").append(v4.ak).append("/")
                    .append(scope).append(",SignedHeaders=").append(signedAndCanonicalList.get(0)).append(",Signature=")
                    .append(signature).toString();
            return new DefaultAuthentication(canonicalRequest, stringToSign, auth);
        } catch (Exception e) {
            throw new ServiceException("has an err when V4 aurhentication ", e);
        }
    }

    private void setNowISOTime(Date headerDate) {
        SimpleDateFormat fmt1 = new SimpleDateFormat(Constants.LONG_DATE_FORMATTER);
        fmt1.setTimeZone(Constants.GMT_TIMEZONE);
        this.nowISOtime = fmt1.format(headerDate);
    }

    private List<String> getSignedAndCanonicalHeaders(Map<String, String> headers) {
        List<String> list = new ArrayList<String>();
        StringBuilder signed = new StringBuilder();
        StringBuilder canonical = new StringBuilder();
        Map<String, List<String>> map = new TreeMap<String, List<String>>();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key == null || "".equals(key) || "connection".equalsIgnoreCase(key)) {
                    continue;
                }

                String lk = key.toLowerCase(Locale.getDefault());
                List<String> values = map.get(lk);
                if (values == null) {
                    values = new ArrayList<String>();
                    map.put(lk, values);
                }
                values.add(value);
            }
            int i = 0;
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (i != 0) {
                    signed.append(";");
                }
                i = 1;
                signed.append(key);
                for (String value : values) {
                    canonical.append(key).append(":").append(value).append("\n");
                }
            }
        }

        list.add(signed.toString());
        list.add(canonical.toString());
        return list;
    }

    private List<String> getCanonicalURIAndQuery(String fulPath) throws ServiceException {
        String url = "";
        String query = "";
        String[] pathStrings = fulPath.split("[?]");
        if (pathStrings.length > 0) {
            url += pathStrings[0];
        }
        if (pathStrings.length > 1) {
            String[] uri = pathStrings[1].split("[&]");
            Map<String, String> map = new TreeMap<String, String>();
            for (int i = 0; i < uri.length; i++) {
                String[] kvStrings = uri[i].split("[=]");
                String key = kvStrings[0];
                String val = "";
                if (kvStrings.length > 1) {
                    val = kvStrings[1];
                }
                map.put(key, val);
            }
            int j = 0;

            StringBuilder tempStr = new StringBuilder();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (j != 0) {
                    tempStr.append("&");
                }
                j = 1;
                tempStr.append(key.toString()).append("=").append(value.toString());
            }

            query = tempStr.toString();

        }
        List<String> list = new ArrayList<String>();
        list.add(url);
        list.add(query);
        return list;
    }

    private String getScope() {
        return new StringBuilder().append(this.nowISOtime.split("T")[0]).append("/").append(this.region).append("/")
                .append(Constants.SERVICE).append("/").append(Constants.REQUEST_TAG).toString();
    }

    private String getCanonicalRequest(String method, String fulPath, List<String> canonical) throws ServiceException {
        List<String> list = this.getCanonicalURIAndQuery(fulPath);
        StringBuilder outPut = new StringBuilder(method).append("\n").append(list.get(0)).append("\n")
                .append(list.get(1)).append("\n").append(canonical.get(1)).append("\n").append(canonical.get(0))
                .append("\n").append(V4Authentication.CONTENT_SHA256);
        return outPut.toString();
    }

    private byte[] getSigningKey() throws ServiceException {
        String shortDate = this.nowISOtime.split("[T]")[0];
        String keyString = "AWS4" + this.sk;
        try {
            byte[] dateKey = V4Authentication.hmacSha256Encode(keyString.getBytes(StandardCharsets.UTF_8),
                    shortDate);
            byte[] dateRegionKey = V4Authentication.hmacSha256Encode(dateKey, this.region);
            byte[] dateRegionServiceKey = V4Authentication.hmacSha256Encode(dateRegionKey, Constants.SERVICE);
            return V4Authentication.hmacSha256Encode(dateRegionServiceKey, Constants.REQUEST_TAG);
        } catch (Exception e) {
            throw new ServiceException("Get sign string for v4 aurhentication error", e);
        }
    }

    public static byte[] hmacSha256Encode(byte[] key, String data)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
        Mac sha256HMAC = Mac.getInstance(Constants.HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretkey = new SecretKeySpec(key, Constants.HMAC_SHA256_ALGORITHM);
        sha256HMAC.init(secretkey);
        return sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha256encode(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest;
        byte[] hash = null;
        digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));

        return hash;
    }

    public static String byteToHex(byte[] hash) {
        return ServiceUtils.toHex(hash);
    }

}
