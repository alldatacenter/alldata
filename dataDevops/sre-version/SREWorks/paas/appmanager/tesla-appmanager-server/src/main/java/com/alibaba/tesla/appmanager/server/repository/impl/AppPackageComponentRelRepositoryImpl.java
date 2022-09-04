package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageComponentRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppPackageComponentRelDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class AppPackageComponentRelRepositoryImpl implements AppPackageComponentRelRepository {

    @Autowired
    private AppPackageComponentRelDOMapper appPackageComponentRelMapper;

    @Override
    public long countByCondition(AppPackageComponentRelQueryCondition condition) {
        return appPackageComponentRelMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppPackageComponentRelQueryCondition condition) {
        return appPackageComponentRelMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(AppPackageComponentRelDO record) {
        return appPackageComponentRelMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppPackageComponentRelDO> selectByCondition(AppPackageComponentRelQueryCondition condition) {
        AppPackageComponentRelDOExample example = buildExample(condition);
        condition.doPagination();
        return appPackageComponentRelMapper.selectByExample(example);
    }

    @Override
    public int updateByCondition(AppPackageComponentRelDO record, AppPackageComponentRelQueryCondition condition) {
        return appPackageComponentRelMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private AppPackageComponentRelDOExample buildExample(AppPackageComponentRelQueryCondition condition) {
        AppPackageComponentRelDOExample example = new AppPackageComponentRelDOExample();
        AppPackageComponentRelDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (Objects.nonNull(condition.getAppPackageId())) {
            criteria.andAppPackageIdEqualTo(condition.getAppPackageId());
        }
        if (Objects.nonNull(condition.getComponentPackageId())) {
            criteria.andComponentPackageIdEqualTo(condition.getComponentPackageId());
        }

        return example;
    }

    private AppPackageComponentRelDO insertDate(AppPackageComponentRelDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppPackageComponentRelDO updateDate(AppPackageComponentRelDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
