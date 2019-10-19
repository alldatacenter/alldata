package com.platform.service;

import com.platform.mall.dto.admin.OssCallbackResult;
import com.platform.mall.dto.admin.OssPolicyResult;

import javax.servlet.http.HttpServletRequest;

/**
 * oss上传管理Service
 * Created by wulinhao on 2019/9/17.
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
