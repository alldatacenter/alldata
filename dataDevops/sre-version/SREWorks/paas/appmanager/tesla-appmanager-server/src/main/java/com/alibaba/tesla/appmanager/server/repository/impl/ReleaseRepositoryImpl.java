package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ReleaseRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ReleaseQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ReleaseDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ReleaseRepositoryImpl implements ReleaseRepository {

    @Autowired
    private ReleaseDOMapper mapper;

    @Override
    public long countByCondition(ReleaseQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ReleaseQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ReleaseDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ReleaseDO> selectByCondition(ReleaseQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public ReleaseDO getByCondition(ReleaseQueryCondition condition) {
        List<ReleaseDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple release found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ReleaseDO record, ReleaseQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ReleaseDOExample buildExample(ReleaseQueryCondition condition) {
        ReleaseDOExample example = new ReleaseDOExample();
        ReleaseDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getReleaseId())) {
            criteria.andReleaseIdEqualTo(condition.getReleaseId());
        }
        if (StringUtils.isNotBlank(condition.getReleaseName())) {
            criteria.andReleaseNameEqualTo(condition.getReleaseName());
        }
        return example;
    }

    private ReleaseDO insertDate(ReleaseDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ReleaseDO updateDate(ReleaseDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
