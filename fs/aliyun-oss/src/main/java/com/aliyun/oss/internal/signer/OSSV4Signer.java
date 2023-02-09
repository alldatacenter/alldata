package com.aliyun.oss.internal.signer;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.common.utils.StringUtils;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.SignParameters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class OSSV4Signer extends OSSSignerBase {
    private static final List<String> DEFAULT_SIGNED_HEADERS = Arrays.asList(HttpHeaders.CONTENT_TYPE.toLowerCase(), HttpHeaders.CONTENT_MD5.toLowerCase());
    // ISO 8601 format
    private static final String ISO8601_DATETIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final String ISO8601_DATE_FORMAT = "yyyyMMdd";
    private static final String SEPARATOR_BACKSLASH = "/";

    private static final String OSS4_HMAC_SHA256 = "OSS4-HMAC-SHA256";
    private static final String TERMINATOR = "aliyun_v4_request";
    private static final String SECRET_KEY_PREFIX = "aliyun_v4";
    Set<String> additionalSignedHeaders;
    private Date requestDateTime;

    protected OSSV4Signer(OSSSignerParams signerParams) {
        super(signerParams);
    }

    private static DateFormat getIso8601DateTimeFormat() {
        SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATETIME_FORMAT, Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df;
    }

    private static DateFormat getIso8601DateFormat() {
        SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATE_FORMAT, Locale.US);
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df;
    }

    private String getDateTime() {
        return getIso8601DateTimeFormat().format(requestDateTime);
    }

    private String getDate() {
        return getIso8601DateFormat().format(requestDateTime);
    }

    private boolean hasDefaultSignedHeaders(String header) {
        if (DEFAULT_SIGNED_HEADERS.contains(header)) {
            return true;
        }
        return header.startsWith(OSSHeaders.OSS_PREFIX);
    }

    private boolean hasSignedHeaders(String header) {
        if (hasDefaultSignedHeaders(header)) {
            return true;
        }
        return additionalSignedHeaders.contains(header);
    }

    private boolean hasAdditionalSignedHeaders() {
        return (additionalSignedHeaders != null) && !additionalSignedHeaders.isEmpty();
    }

    private TreeMap<String, String> buildSortedHeadersMap(Map<String, String> headers) {
        TreeMap<String, String> orderMap = new TreeMap<String, String>();
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String key = header.getKey().toLowerCase();
                if (hasSignedHeaders(key)) {
                    orderMap.put(key, header.getValue());
                }
            }
        }
        return orderMap;
    }

    private void resolveAdditionalSignedHeaders(RequestMessage request) {
        if (request.getOriginalRequest().getAdditionalHeaderNames().isEmpty()) {
            additionalSignedHeaders = request.getOriginalRequest().getAdditionalHeaderNames();
            return;
        }
        Set<String> signedHeaders = new TreeSet<String>();
        for (String additionalHeader : request.getOriginalRequest().getAdditionalHeaderNames()) {
            String additionalHeaderKey = additionalHeader.toLowerCase();
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                String headerKey = header.getKey().toLowerCase();
                if(headerKey.equals(additionalHeaderKey) && !hasDefaultSignedHeaders(additionalHeaderKey)){
                    signedHeaders.add(additionalHeaderKey);
                }
            }
        }
        additionalSignedHeaders = signedHeaders;
    }

    private void addSignedHeaderIfNeeded(RequestMessage request) {
        Set<String> signedHeaders = additionalSignedHeaders;
        if (signedHeaders.contains(OSSHeaders.HOST.toLowerCase()) &&
                !request.getHeaders().containsKey(OSSHeaders.HOST)) {
            request.addHeader(OSSHeaders.HOST, request.getEndpoint().getHost());
        }
    }

    private void addOSSContentSha256Header(RequestMessage request) {
        request.addHeader(OSSHeaders.OSS_CONTENT_SHA256, "UNSIGNED-PAYLOAD");
    }

    @Override
    protected void addDateHeaderIfNeeded(RequestMessage request) {
        Date now = new Date();
        if (signerParams.getTickOffset() != 0) {
            now.setTime(now.getTime() + signerParams.getTickOffset());
        }
        requestDateTime = now;
        request.getHeaders().put(OSSHeaders.OSS_DATE, getDateTime());
    }

    private String buildCanonicalRequest(RequestMessage request) {
        String method = request.getMethod().toString();
        String resourcePath = signerParams.getResourcePath();

        StringBuilder canonicalString = new StringBuilder();

        //http method + "\n"
        canonicalString.append(method).append(SignParameters.NEW_LINE);

        //Canonical URI + "\n"
        canonicalString.append(HttpUtil.urlEncode(resourcePath, true)).append(SignParameters.NEW_LINE);

        //Canonical Query String + "\n" +
        Map<String, String> parameters = request.getParameters();
        TreeMap<String, String> orderMap = new TreeMap<String, String>();
        if (parameters != null) {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                orderMap.put(HttpUtil.urlEncode(StringUtils.trim(param.getKey()), false), HttpUtil.urlEncode(StringUtils.trim(param.getValue()), false));
            }
        }
        String separator = "";
        StringBuilder canonicalPart = new StringBuilder();
        for (Map.Entry<String, String> param : orderMap.entrySet()) {
            canonicalPart.append(separator).append(param.getKey());
            if (param.getValue() != null && !param.getValue().isEmpty()) {
                canonicalPart.append("=").append(param.getValue());
            }
            separator = "&";
        }
        canonicalString.append(canonicalPart).append(SignParameters.NEW_LINE);

        //Canonical Headers + "\n" +
        orderMap = buildSortedHeadersMap(request.getHeaders());
        canonicalPart = new StringBuilder();
        for (Map.Entry<String, String> param : orderMap.entrySet()) {
            canonicalPart.append(param.getKey()).append(":").append(param.getValue().trim()).append(SignParameters.NEW_LINE);
        }
        canonicalString.append(canonicalPart).append(SignParameters.NEW_LINE);

        //Additional Headers + "\n" +
        String canonicalPartStr = StringUtils.join(";", additionalSignedHeaders);
        canonicalString.append(canonicalPartStr).append(SignParameters.NEW_LINE);

        //Hashed PayLoad
        String hashedPayLoad = request.getHeaders().get(OSSHeaders.OSS_CONTENT_SHA256);
        canonicalString.append(hashedPayLoad);

        return canonicalString.toString();
    }

    private String getRegion() {
        if (signerParams.getCloudBoxId() != null) {
            return signerParams.getCloudBoxId();
        }
        return signerParams.getRegion();
    }

    private String getProduct() {
        return signerParams.getProduct();
    }

    private String buildScope() {
        return getDate() + SEPARATOR_BACKSLASH +
                getRegion() + SEPARATOR_BACKSLASH +
                getProduct() + SEPARATOR_BACKSLASH +
                TERMINATOR;
    }

    private String buildStringToSign(String canonicalString) {
        return OSS4_HMAC_SHA256 + SignParameters.NEW_LINE +
                getDateTime() + SignParameters.NEW_LINE +
                buildScope() + SignParameters.NEW_LINE +
                BinaryUtil.toHex(BinaryUtil.calculateSha256(canonicalString.getBytes(StringUtils.UTF8)));
    }

    private byte[] buildSigningKey() {
        ServiceSignature signature = ServiceSignature.create("HmacSHA256");
        byte[] signingSecret = (SECRET_KEY_PREFIX + signerParams.getCredentials().getSecretAccessKey()).getBytes(StringUtils.UTF8);
        byte[] signingDate = signature.computeHash(signingSecret, getDate().getBytes(StringUtils.UTF8));
        byte[] signingRegion = signature.computeHash(signingDate, getRegion().getBytes(StringUtils.UTF8));
        byte[] signingService = signature.computeHash(signingRegion, getProduct().getBytes(StringUtils.UTF8));
//        System.out.println("signingSecret:\n" + BinaryUtil.toHex(signingSecret));
//        System.out.println("signingDate:\n" + BinaryUtil.toHex(signingDate));
//        System.out.println("signingRegion:\n" + BinaryUtil.toHex(signingRegion));
//        System.out.println("signingService:\n" + BinaryUtil.toHex(signingService));
        return signature.computeHash(signingService, TERMINATOR.getBytes(StringUtils.UTF8));
    }

    private String buildSignature(byte[] signingKey, String stringToSign) {
        byte[] result = ServiceSignature.create("HmacSHA256").computeHash(signingKey, stringToSign.getBytes(StringUtils.UTF8));
        return BinaryUtil.toHex(result);
    }

    private String buildAuthorization(String signature) {
        String credential = "Credential=" + signerParams.getCredentials().getAccessKeyId() + SEPARATOR_BACKSLASH + buildScope();
        String signedHeaders = !hasAdditionalSignedHeaders() ? "" : ",AdditionalHeaders=" + StringUtils.join(";", additionalSignedHeaders);
        String sign = ",Signature=" + signature;
        return "OSS4-HMAC-SHA256 " + credential + signedHeaders + sign;
    }

    @Override
    protected void addAuthorizationHeader(RequestMessage request) {
        String canonicalRequest = buildCanonicalRequest(request);
        String stringToSign = buildStringToSign(canonicalRequest);
        byte[] signingKey = buildSigningKey();
        String signature = buildSignature(signingKey, stringToSign);
        String authorization = buildAuthorization(signature);

//        System.out.println("canonicalRequest:\n" + canonicalRequest);
//        System.out.println("stringToSign:\n" + stringToSign);
//        System.out.println("signingKey:\n" + BinaryUtil.toHex(signingKey));
//        System.out.println("signature:\n" + signature);
//        System.out.println("authorization:\n" + authorization);

        request.addHeader(OSSHeaders.AUTHORIZATION, authorization);
    }

    @Override
    public void sign(RequestMessage request) throws ClientException {
        addDateHeaderIfNeeded(request);
        if (isAnonymous()) {
            return;
        }
        resolveAdditionalSignedHeaders(request);
        addSignedHeaderIfNeeded(request);
        addSecurityTokenHeaderIfNeeded(request);
        addOSSContentSha256Header(request);
        addAuthorizationHeader(request);
    }
}
