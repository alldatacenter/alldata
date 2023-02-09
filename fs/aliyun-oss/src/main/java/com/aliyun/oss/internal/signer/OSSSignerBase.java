package com.aliyun.oss.internal.signer;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.internal.OSSHeaders;

import java.util.Date;

public abstract class OSSSignerBase implements RequestSigner {
    protected final OSSSignerParams signerParams;

    protected OSSSignerBase(OSSSignerParams signerParams) {
        this.signerParams = signerParams;
    }

    protected void addDateHeaderIfNeeded(RequestMessage request) {
        Date now = new Date();
        if (signerParams.getTickOffset() != 0) {
            now.setTime(now.getTime() + signerParams.getTickOffset());
        }
        request.getHeaders().put(OSSHeaders.DATE, DateUtil.formatRfc822Date(now));
    }

    protected void addSecurityTokenHeaderIfNeeded(RequestMessage request) {
        Credentials cred = signerParams.getCredentials();
        if (cred.useSecurityToken() && !request.isUseUrlSignature()) {
            request.addHeader(OSSHeaders.OSS_SECURITY_TOKEN, cred.getSecurityToken());
        }
    }

    protected boolean isAnonymous() {
        Credentials cred = signerParams.getCredentials();
        if (cred.getAccessKeyId().length() > 0 && cred.getSecretAccessKey().length() > 0) {
            return false;
        }
        return true;
    }

    protected void addAuthorizationHeader(RequestMessage request) {
    }

    @Override
    public void sign(RequestMessage request) throws ClientException {
        addDateHeaderIfNeeded(request);
        if (isAnonymous()) {
            return;
        }
        addSecurityTokenHeaderIfNeeded(request);
        addAuthorizationHeader(request);
    }

    public static RequestSigner createRequestSigner(SignVersion version, OSSSignerParams signerParams) {
        if (SignVersion.V4.equals(version)) {
            return new OSSV4Signer(signerParams);
        } else if (SignVersion.V2.equals(version)) {
            return new OSSV2Signer(signerParams);
        } else {
            return new OSSV1Signer(signerParams);
        }
    }
}
