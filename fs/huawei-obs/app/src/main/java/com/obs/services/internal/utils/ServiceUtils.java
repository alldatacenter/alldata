/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ObsProperties;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.ext.ExtObsConfiguration;
import com.obs.services.internal.ext.ExtObsConstraint;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpProtocolTypeEnum;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceUtils {
    private static final ILogger log = LoggerBuilder.getLogger(ServiceUtils.class);

    protected static final String ISO_8601_TIME_PARSER_STRING = Constants.EXPIRATION_DATE_FORMATTER;
    protected static final String ISO_8601_TIME_MIDNING_PARSER_STRING = "yyyy-MM-dd'T'00:00:00'Z'";
    protected static final String ISO_8601_TIME_PARSER_WALRUS_STRING = "yyyy-MM-dd'T'HH:mm:ss";
    protected static final String RFC_822_TIME_PARSER_STRING = Constants.HEADER_DATE_FORMATTER;
    protected static final String ISO_8601_DATE_PARSER_STRING = "yyyy-MM-dd";

    private static Pattern pattern = Pattern
            .compile("^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$");

    public static boolean isValid(String s) {
        return s != null && !s.equals("");
    }

    public static boolean isValid2(String s) {
        return s != null && !s.equals("");
    }

    public static String toValid(String s) {
        return s == null ? "" : s;
    }

    public static void assertParameterNotNull(String value, String errorMessage) {
        if (!isValid(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void assertParameterNotNull2(String value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void assertParameterNotNull(Object value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void assertParameterNotNegative(long value, String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static Date parseIso8601Date(String dateString) throws ParseException {
        ParseException exception;
        SimpleDateFormat iso8601TimeParser = new SimpleDateFormat(ISO_8601_TIME_PARSER_STRING);
        TimeZone gmt = Constants.GMT_TIMEZONE;
        iso8601TimeParser.setTimeZone(gmt);
        try {
            return iso8601TimeParser.parse(dateString);
        } catch (ParseException e) {
            exception = e;
        }
        SimpleDateFormat iso8601TimeParserWalrus = new SimpleDateFormat(ISO_8601_TIME_PARSER_WALRUS_STRING);
        iso8601TimeParserWalrus.setTimeZone(gmt);
        try {
            return iso8601TimeParserWalrus.parse(dateString);
        } catch (ParseException e) {

            exception = e;
        }
        SimpleDateFormat iso8601DateParser = new SimpleDateFormat(ISO_8601_DATE_PARSER_STRING);
        iso8601DateParser.setTimeZone(gmt);
        try {
            return iso8601DateParser.parse(dateString);
        } catch (Exception e) {
            log.warn("date parser failed.", e);
        }
        throw exception;
    }

    public static String formatIso8601Date(Date date) {
        SimpleDateFormat iso8601TimeParser = new SimpleDateFormat(ISO_8601_TIME_PARSER_STRING);
        iso8601TimeParser.setTimeZone(Constants.GMT_TIMEZONE);
        return iso8601TimeParser.format(date);
    }

    public static String formatIso8601MidnightDate(Date date) {
        SimpleDateFormat iso8601TimeParser = new SimpleDateFormat(ISO_8601_TIME_MIDNING_PARSER_STRING);
        iso8601TimeParser.setTimeZone(Constants.GMT_TIMEZONE);
        return iso8601TimeParser.format(date);
    }

    public static Date parseRfc822Date(String dateString) throws ParseException {
        SimpleDateFormat rfc822TimeParser = new SimpleDateFormat(RFC_822_TIME_PARSER_STRING, Locale.US);
        rfc822TimeParser.setTimeZone(Constants.GMT_TIMEZONE);
        return rfc822TimeParser.parse(dateString);
    }

    public static String formatRfc822Date(Date date) {
        SimpleDateFormat rfc822TimeParser = new SimpleDateFormat(RFC_822_TIME_PARSER_STRING, Locale.US);
        rfc822TimeParser.setTimeZone(Constants.GMT_TIMEZONE);
        return rfc822TimeParser.format(date);
    }

    public static String signWithHmacSha1(String sk, String canonicalString) throws ServiceException {
        SecretKeySpec signingKey;
        signingKey = new SecretKeySpec(sk.getBytes(StandardCharsets.UTF_8), Constants.HMAC_SHA1_ALGORITHM);

        Mac mac;
        try {
            mac = Mac.getInstance(Constants.HMAC_SHA1_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("Could not find sha1 algorithm", e);
        }
        try {
            mac.init(signingKey);
        } catch (InvalidKeyException e) {
            throw new ObsException("Could not initialize the MAC algorithm", e);
        }

        return ServiceUtils.toBase64(mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8)));
    }

    private static String transRealKey(String headerPrefix, String metadataPrefix, String key, boolean needDecode)
            throws UnsupportedEncodingException {
        if (key.toLowerCase().startsWith(metadataPrefix)) {
            key = key.substring(metadataPrefix.length());
            if (needDecode) {
                key = URLDecoder.decode(key, Constants.DEFAULT_ENCODING);
            }
            if (log.isDebugEnabled()) {
                log.debug("Removed metadata header prefix " + metadataPrefix + " from key: " + key
                        + "=>" + key);
            }
        } else {
            key = key.substring(headerPrefix.length());
        }
        return key;
    }

    public static Map<String, String> cleanRestMetadataMapV2(Map<String, String> metadata, String headerPrefix,
                                                             String metadataPrefix, boolean needDecode) {
        if (log.isDebugEnabled()) {
            log.debug("Cleaning up REST metadata items");
        }
        Map<String, String> cleanMap = new IdentityHashMap<>();

        if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Trim prefixes from keys.
                key = key != null ? key : "";
                if (key.toLowerCase().startsWith(headerPrefix)) {
                    try {
                        key = transRealKey(headerPrefix, metadataPrefix, key, needDecode);
                        if (needDecode) {
                            value = URLDecoder.decode(value, Constants.DEFAULT_ENCODING);
                        }
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Error to decode value of key:" + key);
                        }
                    }

                } else if (key.toLowerCase().startsWith(Constants.OBS_HEADER_PREFIX)) {
                    try {
                        key = transRealKey(Constants.OBS_HEADER_PREFIX,
                                Constants.OBS_HEADER_META_PREFIX, key, needDecode);
                        if (needDecode) {
                            value = URLDecoder.decode(value, Constants.DEFAULT_ENCODING);
                        }
                    } catch (UnsupportedEncodingException | IllegalArgumentException  e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Error to decode value of key:" + key);
                        }
                    }
                } else if (Constants.ALLOWED_RESPONSE_HTTP_HEADER_METADATA_NAMES
                        .contains(key.toLowerCase(Locale.getDefault()))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Leaving HTTP header item unchanged: " + key + "=" + value);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring metadata item: " + key + "=" + value);
                    }
                    continue;
                }

                // FIXME
                cleanMap.put(key, value);
            }
        }
        return cleanMap;
    }

    public static Map<String, Object> cleanUserMetadata(Map<String, Object> originalHeaders, boolean decodeHeaders) {
        Map<String, Object> userMetadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Object> entry : originalHeaders.entrySet()) {
            String key = entry.getKey();
            if (key.toLowerCase().startsWith("x-obs-meta-") || key.toLowerCase().startsWith("x-amz-meta-")) {
                Object originalValue = originalHeaders.get(key);
                try {
                    if (originalValue instanceof ArrayList) {
                        cleanListMetadata(originalHeaders, decodeHeaders, userMetadata, key);
                    } else {
                        if (decodeHeaders) {
                            userMetadata.put(key.substring(11), URLDecoder.decode((String) originalValue,
                                    Constants.DEFAULT_ENCODING));
                        } else {
                            userMetadata.put(key.substring(11), originalHeaders.get(key));
                        }
                    }
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Error to decode value of key:" + key);
                    }
                }
            }
        }
        return userMetadata;
    }

    public static void cleanListMetadata(Map<String, Object> originalHeaders, boolean decodeHeaders,
                                   Map<String, Object> userMetadata, String key)
            throws UnsupportedEncodingException {
        List<String> cleanedValue = new ArrayList<>();
        for (Object v : (List<?>) originalHeaders.get(key)) {
            if (decodeHeaders) {
                cleanedValue.add(URLDecoder.decode((String) v, Constants.DEFAULT_ENCODING));
            } else {
                cleanedValue.add((String) v);
            }
        }
        userMetadata.put(key.substring(11), cleanedValue);
    }

    public static String toHex(byte[] data) {
        if (null == data) {
            return null;
        }
        if (data.length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte datum : data) {
            String hv = Integer.toHexString(datum);
            if (hv.length() < 2) {
                sb.append("0");
            } else if (hv.length() == 8) {
                hv = hv.substring(6);
            }
            sb.append(hv);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    public static byte[] fromHex(String hexData) {
        if (null == hexData) {
            return new byte[0];
        }

        if ((hexData.length() & 1) != 0 || hexData.replaceAll("[a-fA-F0-9]", "").length() > 0) {
            throw new java.lang.IllegalArgumentException("'" + hexData + "' is not a hex string");
        }

        byte[] result = new byte[(hexData.length() + 1) / 2];
        String hexNumber;
        int offset = 0;
        int byteIndex = 0;
        while (offset < hexData.length()) {
            hexNumber = hexData.substring(offset, offset + 2);
            offset += 2;
            result[byteIndex++] = (byte) Integer.parseInt(hexNumber, 16);
        }
        return result;
    }

    public static String toBase64(byte[] data) {
        return ReflectUtils.toBase64(data);
    }

    public static String join(List<?> items, String delimiter, boolean needTrim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i).toString();
            sb.append(needTrim ? item.trim() : item);
            if (i < items.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String join(List<?> items, String delimiter) {
        return join(items, delimiter, false);
    }

    public static byte[] fromBase64(String b64Data) throws UnsupportedEncodingException {
        return ReflectUtils.fromBase64(b64Data);
    }

    public static byte[] computeMD5Hash(InputStream is) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(is);
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("close failed.", e);
                    }
                }
            }
        }
    }

    public static byte[] computeMD5Hash(InputStream is, long length, long offset)
            throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(is);
            if (offset > 0) {
                long skipByte = bis.skip(offset);
                if (log.isDebugEnabled()) {
                    log.debug("computeMD5Hash: Skip " + skipByte + " bytes");
                }
            }
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            long readLen = 0;
            long bufLen = 16384 > length ? length : 16384;
            while (readLen < length && (bytesRead = bis.read(buffer, 0, (int) bufLen)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
                readLen += bytesRead;
                bufLen = (length - readLen) > 16384 ? 16384 : (length - readLen);
            }
            return messageDigest.digest();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("close failed.", e);
                    }
                }
            }
        }
    }

    public static String computeMD5(String data) throws ServiceException {
        try {
            return ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ServiceException("Failed to get MD5 for requestXmlElement:" + data);
        }
    }

    public static byte[] computeMD5Hash(byte[] data) throws NoSuchAlgorithmException, IOException {
        return computeMD5Hash(new ByteArrayInputStream(data));
    }

    public static boolean isBucketNameValidDNSName(String bucketName) {
        if (bucketName == null || bucketName.length() > 63 || bucketName.length() < 3) {
            return false;
        }

        if (!Pattern.matches("^[a-z0-9][a-z0-9.-]+$", bucketName)) {
            return false;
        }

        if (Pattern.matches("(\\d{1,3}\\.){3}\\d{1,3}", bucketName)) {
            return false;
        }

        String[] fragments = bucketName.split("\\.");
        for (String fragment : fragments) {
            if (Pattern.matches("^-.*", fragment) || Pattern.matches(".*-$", fragment)
                    || Pattern.matches("^$", fragment)) {
                return false;
            }
        }

        return true;
    }

    public static String generateHostnameForBucket(String bucketName, boolean pathStyle, String endpoint) {
        if (!isBucketNameValidDNSName(bucketName)) {
            throw new IllegalArgumentException("the bucketName is illegal");
        }

        if (!pathStyle) {
            return bucketName + "." + endpoint;
        } else {
            return endpoint;
        }
    }

    public static XMLReader loadXMLReader() throws ServiceException {
        Exception ex;
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            try {
                reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception e) {
                log.warn("Enable protection for XML External Entity Injection Failed");
            }
            return reader;
        } catch (Exception e) {
            // Ignore failure
            ex = e;
        }

        String[] altXmlReaderClasspaths = new String[]{"org.apache.crimson."
                + "parser.XMLReaderImpl", "org.xmlpull.v1.sax2.Driver"};
        for (String xmlReaderClasspath : altXmlReaderClasspaths) {
            try {
                XMLReader reader = XMLReaderFactory.createXMLReader(xmlReaderClasspath);
                try {
                    reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                    reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                } catch (Exception e) {
                    log.warn("Enable protection for XML External Entity Injection Failed");
                }

                return reader;
            } catch (Exception e) {
                // Ignore failure
            }
        }
        // If we haven't found and returned an XMLReader yet, give up.
        throw new ServiceException("Failed to initialize a SAX XMLReader", ex);
    }

    public static SimpleDateFormat getShortDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(Constants.SHORT_DATE_FORMATTER);
        format.setTimeZone(Constants.GMT_TIMEZONE);
        return format;
    }

    public static SimpleDateFormat getLongDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(Constants.LONG_DATE_FORMATTER);
        format.setTimeZone(Constants.GMT_TIMEZONE);
        return format;
    }

    public static SimpleDateFormat getExpirationDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(Constants.EXPIRATION_DATE_FORMATTER);
        format.setTimeZone(Constants.GMT_TIMEZONE);
        return format;
    }

    public static ObsException changeFromServiceException(ServiceException se) {
        ObsException exception;
        if (se.getResponseCode() < 0) {
            exception = new ObsException("OBS servcie Error Message. " + se.getMessage(), se.getCause());
        } else {
            exception = new ObsException(
                    (se.getMessage() != null ? "Error message:" + se.getMessage() : "") + "OBS servcie Error Message.",
                    se.getXmlMessage(), se.getCause());
            exception.setErrorCode(se.getErrorCode());
            exception.setErrorMessage(se.getErrorMessage() == null ? se.getMessage() : se.getErrorMessage());
            exception.setErrorRequestId(se.getErrorRequestId());
            exception.setErrorHostId(se.getErrorHostId());
            exception.setResponseCode(se.getResponseCode());
            exception.setResponseStatus(se.getResponseStatus());
            exception.setResponseHeaders(se.getResponseHeaders());
            exception.setErrorIndicator(se.getErrorIndicator());
        }
        return exception;
    }

    public static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                if (log.isWarnEnabled()) {
                    log.warn("close failed.", e);
                }
            }
        }
    }

    public static String toString(InputStream in) throws IOException {
        String ret = null;
        if (in != null) {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String temp;
                while ((temp = br.readLine()) != null) {
                    sb.append(temp);
                }
                ret = sb.toString();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
                closeStream(in);
            }
        }
        return ret;
    }

    public static ObsProperties changeFromObsConfiguration(ObsConfiguration config) {
        ObsProperties obsProperties = new ObsProperties();

        String endPoint = config.getEndPoint();

        int index;
        while ((index = endPoint.lastIndexOf("/")) == endPoint.length() - 1) {
            endPoint = endPoint.substring(0, index);
        }

        if (endPoint.startsWith("http://")) {
            config.setHttpsOnly(false);
            endPoint = endPoint.substring("http://".length());
        } else if (endPoint.startsWith("https://")) {
            config.setHttpsOnly(true);
            endPoint = endPoint.substring("https://".length());
        }

        if ((index = endPoint.lastIndexOf(":")) > 0) {
            int port = Integer.parseInt(endPoint.substring(index + 1));
            if (config.isHttpsOnly()) {
                config.setEndpointHttpsPort(port);
            } else {
                config.setEndpointHttpPort(port);
            }
            endPoint = endPoint.substring(0, index);
        }

        Matcher m = pattern.matcher(endPoint);
        if (m.matches()) {
            config.setPathStyle(true);
        }

        if (config.isPathStyle() || config.isCname()) {
            config.setAuthTypeNegotiation(false);
            if (config.getAuthType() == AuthTypeEnum.OBS) {
                config.setAuthType(AuthTypeEnum.V2);
            }
        }

        config.setEndPoint(endPoint);
        setObsProperties(config, obsProperties);

        return obsProperties;
    }

    private static void setObsProperties(ObsConfiguration config, ObsProperties obsProperties) {
        obsProperties.setProperty(ObsConstraint.END_POINT, config.getEndPoint());
        obsProperties.setProperty(ObsConstraint.HTTP_PORT, String.valueOf(config.getEndpointHttpPort()));
        obsProperties.setProperty(ObsConstraint.HTTPS_ONLY, String.valueOf(config.isHttpsOnly()));
        obsProperties.setProperty(ObsConstraint.DISABLE_DNS_BUCKET, String.valueOf(config.isPathStyle()));
        obsProperties.setProperty(ObsConstraint.HTTPS_PORT, String.valueOf(config.getEndpointHttpsPort()));
        obsProperties.setProperty(ObsConstraint.HTTP_SOCKET_TIMEOUT, String.valueOf(config.getSocketTimeout()));
        obsProperties.setProperty(ObsConstraint.HTTP_MAX_CONNECT, String.valueOf(config.getMaxConnections()));
        obsProperties.setProperty(ObsConstraint.HTTP_RETRY_MAX, String.valueOf(config.getMaxErrorRetry()));
        obsProperties.setProperty(ObsConstraint.HTTP_CONNECT_TIMEOUT, String.valueOf(config.getConnectionTimeout()));
        obsProperties.setProperty(ObsConstraint.PROXY_ISABLE, String.valueOf(Boolean.FALSE));
//        obsProperties.setProperty(ObsConstraint.BUFFER_STREAM,
//                String.valueOf(config.getUploadStreamRetryBufferSize() > 0 ? config.getUploadStreamRetryBufferSize()
//                        : ObsConstraint.DEFAULT_BUFFER_STREAM));
        obsProperties.setProperty(ObsConstraint.VALIDATE_CERTIFICATE, String.valueOf(config.isValidateCertificate()));
        obsProperties.setProperty(ObsConstraint.VERIFY_RESPONSE_CONTENT_TYPE,
                String.valueOf(config.isVerifyResponseContentType()));
        obsProperties.setProperty(ObsConstraint.WRITE_BUFFER_SIZE, String.valueOf(config.getWriteBufferSize()));
        obsProperties.setProperty(ObsConstraint.READ_BUFFER_SIZE, String.valueOf(config.getReadBufferSize()));
        obsProperties.setProperty(ObsConstraint.SOCKET_WRITE_BUFFER_SIZE,
                String.valueOf(config.getSocketWriteBufferSize()));
        obsProperties.setProperty(ObsConstraint.SOCKET_READ_BUFFER_SIZE,
                String.valueOf(config.getSocketReadBufferSize()));
        obsProperties.setProperty(ObsConstraint.HTTP_STRICT_HOSTNAME_VERIFICATION,
                String.valueOf(config.isStrictHostnameVerification()));
        obsProperties.setProperty(ObsConstraint.HTTP_IDLE_CONNECTION_TIME,
                String.valueOf(config.getIdleConnectionTime()));
        obsProperties.setProperty(ObsConstraint.HTTP_MAX_IDLE_CONNECTIONS,
                String.valueOf(config.getMaxIdleConnections()));
        obsProperties.setProperty(ObsConstraint.SSL_PROVIDER,
                config.getSslProvider() == null ? "" : config.getSslProvider());
        obsProperties.setProperty(ObsConstraint.KEEP_ALIVE, String.valueOf(config.isKeepAlive()));
        obsProperties.setProperty(ObsConstraint.FS_DELIMITER,
                config.getDelimiter() == null ? "/" : config.getDelimiter());
        obsProperties.setProperty(ObsConstraint.HTTP_PROTOCOL, config.getHttpProtocolType() == null
                ? HttpProtocolTypeEnum.HTTP1_1.getCode() : config.getHttpProtocolType().getCode());

        obsProperties.setProperty(ObsConstraint.IS_CNAME, String.valueOf(config.isCname()));
        obsProperties.setProperty(ObsConstraint.AUTH_TYPE_NEGOTIATION, String.valueOf(config.isAuthTypeNegotiation()));
        if (null != config.getHttpProxy()) {
            obsProperties.setProperty(ObsConstraint.PROXY_ISABLE, String.valueOf(Boolean.TRUE));
            obsProperties.setProperty(ObsConstraint.PROXY_HOST, config.getHttpProxy().getProxyAddr());
            obsProperties.setProperty(ObsConstraint.PROXY_PORT, String.valueOf(config.getHttpProxy().getProxyPort()));
            obsProperties.setProperty(ObsConstraint.PROXY_UNAME, config.getHttpProxy().getProxyUName());
            obsProperties.setProperty(ObsConstraint.PROXY_PAWD, config.getHttpProxy().getUserPasswd());
            obsProperties.setProperty(ObsConstraint.PROXY_DOMAIN, config.getHttpProxy().getDomain());
            obsProperties.setProperty(ObsConstraint.PROXY_WORKSTATION, config.getHttpProxy().getWorkstation());
        }

        if (config instanceof ExtObsConfiguration) {
            // retry in okhttp
            obsProperties.setProperty(ExtObsConstraint.IS_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP,
                    String.valueOf(((ExtObsConfiguration) config).isRetryOnConnectionFailureInOkhttp()));

            // retry on unexpected end exception
            obsProperties.setProperty(ExtObsConstraint.HTTP_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION,
                    String.valueOf(((ExtObsConfiguration) config).getMaxRetryOnUnexpectedEndException()));
        }
        if (null != config.getXmlDocumentBuilderFactoryClass()
                && !config.getXmlDocumentBuilderFactoryClass().trim().equals("")) {
            obsProperties.setProperty(ObsConstraint.OBS_XML_DOC_BUILDER_FACTORY,
                    config.getXmlDocumentBuilderFactoryClass());
        }
    }

    public static Date cloneDateIgnoreNull(Date date) {
        if (null == date) {
            return null;
        } else {
            return (Date) date.clone();
        }
    }

    public static void deleteFileIgnoreException(String path) {
        if (null == path) {
            return;
        }

        if (!deleteFileIgnoreException(new File(path))) {
            log.warn("delete file '" + path + "' failed");
        }
    }

    public static boolean deleteFileIgnoreException(File file) {
        if (null == file) {
            return true;
        }

        if (file.exists() && file.isFile()) {
            return file.delete();
        }

        return true;
    }
}
