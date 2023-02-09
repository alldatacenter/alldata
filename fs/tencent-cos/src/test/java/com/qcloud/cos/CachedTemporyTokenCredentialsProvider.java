package com.qcloud.cos;

import com.qcloud.cos.auth.AbstractCOSCachedCredentialsProvider;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSSessionCredentials;

public class CachedTemporyTokenCredentialsProvider extends AbstractCOSCachedCredentialsProvider {

    private long temporyTokenDuration = 30; 
    
    public CachedTemporyTokenCredentialsProvider(long refreshPeriodSeconds, long temporyTokenDuration) {
        super(refreshPeriodSeconds);
        this.temporyTokenDuration = temporyTokenDuration;
    }

    @Override
    public COSCredentials fetchNewCOSCredentials() {
        TemporyToken temporyToken = AbstractCOSClientTest.fetchTempToken(this.temporyTokenDuration);
        if (temporyToken == null) {
            return null;
        }
        if (temporyToken.getTempSecretId() == null || temporyToken.getTempSecretKey() == null
                || temporyToken.getTempToken() == null) {
            return null;
        }
        COSSessionCredentials credentials =
                new BasicSessionCredentials(temporyToken.getTempSecretId(),
                        temporyToken.getTempSecretKey(), temporyToken.getTempToken());
        return credentials;
    }

    @Override
    public void refresh() {
    }
}
