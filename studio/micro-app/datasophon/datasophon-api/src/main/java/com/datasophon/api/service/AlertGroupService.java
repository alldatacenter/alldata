package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AlertGroupEntity;

/**
 * 告警组表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface AlertGroupService extends IService<AlertGroupEntity> {

    Result getAlertGroupList(Integer clusterId,String alertGroupName, Integer page, Integer pageSize);

    Result saveAlertGroup(AlertGroupEntity alertGroup);
}

