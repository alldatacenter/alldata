package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppMetaDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class AppMetaRepositoryImpl implements AppMetaRepository {

    @Autowired
    private AppMetaDOMapper appMetaMapper;

    @Override
    public long countByCondition(AppMetaQueryCondition condition) {
        return appMetaMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppMetaQueryCondition condition) {
        return appMetaMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(AppMetaDO record) {
        return appMetaMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppMetaDO> selectByCondition(AppMetaQueryCondition condition) {
        AppMetaDOExample example = buildExample(condition);
        condition.doPagination();
        return appMetaMapper.selectByExample(example);
    }

    @Override
    public int updateByCondition(AppMetaDO record, AppMetaQueryCondition condition) {
        return appMetaMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private AppMetaDOExample buildExample(AppMetaQueryCondition condition) {
        AppMetaDOExample example = new AppMetaDOExample();
        AppMetaDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotEmpty(condition.getAppIdLike())) {
            criteria.andAppIdLike("%" + StringEscapeUtils.escapeSql(condition.getAppIdLike()) + "%");
        }
        if (CollectionUtils.isNotEmpty(condition.getAppIdList())) {
            criteria.andAppIdIn(condition.getAppIdList());
        }
        return example;
    }

    private AppMetaDO insertDate(AppMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppMetaDO updateDate(AppMetaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
