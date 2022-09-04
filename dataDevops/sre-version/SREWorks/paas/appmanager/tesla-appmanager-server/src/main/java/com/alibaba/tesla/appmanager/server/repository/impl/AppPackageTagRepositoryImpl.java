package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTagRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppPackageTagDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/11/17.
 */
@Service
@Slf4j
public class AppPackageTagRepositoryImpl implements AppPackageTagRepository {

    @Autowired
    private AppPackageTagDOMapper appPackageTagMapper;

    @Override
    public int insert(AppPackageTagDO record) {
        return appPackageTagMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppPackageTagDO> query(List<Long> appPackageIdList, String tag) {
        if (CollectionUtils.isEmpty(appPackageIdList)) {
            return Collections.emptyList();
        }

        AppPackageTagDOExample example = new AppPackageTagDOExample();
        AppPackageTagDOExample.Criteria criteria = example.createCriteria();
        criteria.andAppPackageIdIn(appPackageIdList);
        if (!StringUtils.isEmpty(tag)) {
            criteria.andTagEqualTo(tag);
        }

        return appPackageTagMapper.selectByExample(example);
    }

    @Override
    public List<AppPackageTagDO> query(List<Long> appPackageIdList) {
        return query(appPackageIdList, "");
    }

    @Override
    public int deleteByCondition(AppPackageTagQueryCondition condition) {
        return appPackageTagMapper.deleteByExample(buildExample(condition));
    }

    private AppPackageTagDOExample buildExample(AppPackageTagQueryCondition condition) {
        AppPackageTagDOExample example = new AppPackageTagDOExample();
        AppPackageTagDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (condition.getAppPackageId() != null && condition.getAppPackageId() > 0) {
            criteria.andAppPackageIdEqualTo(condition.getAppPackageId());
        }
        if (CollectionUtils.isNotEmpty(condition.getTagList())) {
            criteria.andTagIn(condition.getTagList());
        }
        return example;
    }

    private AppPackageTagDO insertDate(AppPackageTagDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }
}
