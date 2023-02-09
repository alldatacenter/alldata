package com.aliyun.oss.internal.signer;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.SignUtils;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import java.net.URI;

public class OSSV1Signer extends OSSSignerBase {

    public OSSV1Signer(OSSSignerParams signerParams) {
        super(signerParams);
    }

    @Override
    protected void addAuthorizationHeader(RequestMessage request) {
        Credentials cred = signerParams.getCredentials();
        String accessKeyId = cred.getAccessKeyId();
        String secretAccessKey = cred.getSecretAccessKey();
        String signature;
        signature = SignUtils.buildSignature(secretAccessKey, request.getMethod().toString(), signerParams.getResourcePath(), request);
        request.addHeader(OSSHeaders.AUTHORIZATION, SignUtils.composeRequestAuthorization(accessKeyId, signature));
    }
}
