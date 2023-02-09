package com.aliyun.oss.internal.signer;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.SignV2Utils;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import java.net.URI;

public class OSSV2Signer extends OSSSignerBase {

    protected OSSV2Signer(OSSSignerParams signerParams) {
        super(signerParams);
    }

    @Override
    protected void addAuthorizationHeader(RequestMessage request) {
        Credentials cred = signerParams.getCredentials();
        String accessKeyId = cred.getAccessKeyId();
        String secretAccessKey = cred.getSecretAccessKey();
        String signature;
        signature = SignV2Utils.buildSignature(secretAccessKey, request.getMethod().toString(), signerParams.getResourcePath(), request);
        request.addHeader(OSSHeaders.AUTHORIZATION, SignV2Utils.composeRequestAuthorization(accessKeyId,signature, request));
    }
}
