package com.alibaba.tesla.appmanager.server.service.imagebuilder;

import com.alibaba.tesla.appmanager.domain.req.imagebuilder.ImageBuilderCreateReq;
import com.alibaba.tesla.appmanager.domain.res.imagebuilder.ImageBuilderCreateRes;

import java.util.concurrent.Future;

/**
 * 镜像构建服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ImageBuilderService {

    /**
     * 镜像构建
     *
     * @param request 请求配置
     * @return Future 返回结果
     */
    Future<ImageBuilderCreateRes> build(ImageBuilderCreateReq request);
}
