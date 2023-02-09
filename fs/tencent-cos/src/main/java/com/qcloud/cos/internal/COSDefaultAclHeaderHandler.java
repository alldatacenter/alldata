package com.qcloud.cos.internal;

import com.qcloud.cos.Headers;
import com.qcloud.cos.http.CosHttpResponse;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.CannedAccessControlList;

import java.util.Map;

public class COSDefaultAclHeaderHandler implements HeaderHandler<AccessControlList> {
    @Override
    public void handle(AccessControlList result, CosHttpResponse response) {
        Map<String, String> headers = response.getHeaders();
        if(headers.containsKey(Headers.COS_CANNED_ACL) &&
                headers.get(Headers.COS_CANNED_ACL).equals(CannedAccessControlList.Default.toString())) {
            result.setExistDefaultAcl(true);
        }
    }
}