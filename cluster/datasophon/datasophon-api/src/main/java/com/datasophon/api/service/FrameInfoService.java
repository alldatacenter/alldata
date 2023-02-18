package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.FrameInfoEntity;

/**
 * 集群框架表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface FrameInfoService extends IService<FrameInfoEntity> {

    Result getAllClusterFrame();
}

