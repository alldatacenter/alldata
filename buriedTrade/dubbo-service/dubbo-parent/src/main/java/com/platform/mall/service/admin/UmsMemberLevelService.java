package com.platform.mall.service.admin;

import com.platform.mall.entity.admin.UmsMemberLevel;

import java.util.List;

/**
 * 会员等级管理Service
 * @author AllDataDC
 */
public interface UmsMemberLevelService {
    /**
     * 获取所有会员登录
     *
     * @param defaultStatus 是否为默认会员
     */
    List<UmsMemberLevel> list(Integer defaultStatus);
}
