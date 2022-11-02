package com.platform.service;

import com.platform.mall.dto.admin.OssCallbackResult;
import com.platform.mall.dto.admin.OssPolicyResult;

import javax.servlet.http.HttpServletRequest;

/**
 * oss上传管理Service
 * @author AllDataDC
 */
public interface OssService {
    /**
     * oss上传策略生成
     */
    OssPolicyResult policy();

    /**
     * oss上传成功回调
     */
    OssCallbackResult callback(HttpServletRequest request);
}
