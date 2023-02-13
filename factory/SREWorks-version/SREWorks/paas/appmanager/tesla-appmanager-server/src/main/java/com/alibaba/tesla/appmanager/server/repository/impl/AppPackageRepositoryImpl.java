package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageVersionCountDTO;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageVersionCountQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AppPackageDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class AppPackageRepositoryImpl implements AppPackageRepository {

    @Autowired
    private AppPackageDOMapper appPackageDOMapper;

    @Override
    public long countByCondition(AppPackageQueryCondition condition) {
        return appPackageDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AppPackageQueryCondition condition) {
        return appPackageDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return appPackageDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(AppPackageDO record) {
        return appPackageDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<AppPackageDO> selectByCondition(AppPackageQueryCondition condition) {
        List<String> tags = condition.getTags();
        String appId = condition.getAppId();
        if (!CollectionUtils.isEmpty(tags) && StringUtils.isEmpty(appId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "appId is required when use tags in query");
        }

        AppPackageDOExample example = buildExample(condition);
        condition.doPagination();
        if (condition.isWithBlobs()) {
            if (!CollectionUtils.isEmpty(tags)) {
                return appPackageDOMapper.selectByTagsWithBLOBs(appId, tags, tags.size(), example);
            }
            return appPackageDOMapper.selectByExampleWithBLOBs(example);
        } else {
            if (!CollectionUtils.isEmpty(tags)) {
                return appPackageDOMapper.selectByTags(appId, tags, tags.size(), example);
            }
            return appPackageDOMapper.selectByExample(example);
        }
    }

    @Override
    public AppPackageDO getByCondition(AppPackageQueryCondition condition) {
        List<AppPackageDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple app packages found with condition %s", JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(AppPackageDO record, AppPackageQueryCondition condition) {
        return appPackageDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKeySelective(AppPackageDO record) {
        return appPackageDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    @Override
    public List<AppPackageVersionCountDTO> countPackageByCondition(AppPackageVersionCountQueryCondition condition) {
        List<String> appIds = condition.getAppIds();
        String tag = condition.getTag();
        return appPackageDOMapper.countAppPackageVersion(appIds, tag);
    }

    private AppPackageDOExample buildExample(AppPackageQueryCondition condition) {
        AppPackageDOExample example = new AppPackageDOExample();
        AppPackageDOExample.Criteria criteria = example.createCriteria();
        if (condition.getId() != null && condition.getId() > 0) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getPackageCreator())) {
            criteria.andPackageCreatorEqualTo(condition.getPackageCreator());
        }
        if (StringUtils.isNotBlank(condition.getPackageVersion())) {
            criteria.andPackageVersionEqualTo(condition.getPackageVersion());
        }
        if (StringUtils.isNotBlank(condition.getPackageVersionGreaterThan())) {
            criteria.andPackageVersionGreaterThan(condition.getPackageVersionGreaterThan());
        }
        if (StringUtils.isNotBlank(condition.getPackageVersionLessThan())) {
            criteria.andPackageVersionLessThan(condition.getPackageVersionLessThan());
        }
        return example;
    }

    private AppPackageDO insertDate(AppPackageDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AppPackageDO updateDate(AppPackageDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
