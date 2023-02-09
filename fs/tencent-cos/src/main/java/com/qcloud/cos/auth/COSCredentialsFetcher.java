package com.qcloud.cos.auth;

import com.qcloud.cos.exception.CosClientException;

public interface COSCredentialsFetcher {
    COSCredentials fetch() throws CosClientException;

    COSCredentials fetch(int retryTimes) throws CosClientException;
}
