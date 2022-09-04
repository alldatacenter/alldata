package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtAppInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.RtAppInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDOExample;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtAppInstanceDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtAppInstanceRepositoryImpl implements RtAppInstanceRepository {

    @Autowired
    private RtAppInstanceHistoryRepository historyRepository;

    @Autowired
    private RtAppInstanceDOMapper mapper;

    @Override
    public long countByCondition(RtAppInstanceQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtAppInstanceQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtAppInstanceDO record) {
        int inserted = mapper.insertSelective(insertDate(record));
        historyRepository.insert(RtAppInstanceHistoryDO.builder()
                .appInstanceId(record.getAppInstanceId())
                .status(record.getStatus())
                .version(record.getVersion())
                .build());
        return inserted;
    }

    @Override
    public List<RtAppInstanceDO> selectByCondition(RtAppInstanceQueryCondition condition) {
        condition.doPagination();
        String optionKey = condition.getOptionKey();
        String optionValue = condition.getOptionValue();
        if (StringUtils.isNotEmpty(optionKey) && StringUtils.isNotEmpty(optionValue)) {
            return mapper.selectByExampleAndOption(buildExample(condition), optionKey, optionValue);
        } else {
            return mapper.selectByExample(buildExample(condition));
        }
    }

    @Override
    public RtAppInstanceDO getByCondition(RtAppInstanceQueryCondition condition) {
        List<RtAppInstanceDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple app instance found with query condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(RtAppInstanceDO record, RtAppInstanceQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private RtAppInstanceDOExample buildExample(RtAppInstanceQueryCondition condition) {
        RtAppInstanceDOExample example = new RtAppInstanceDOExample();
        RtAppInstanceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppInstanceId())) {
            criteria.andAppInstanceIdEqualTo(condition.getAppInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            if (condition.getAppId().contains(",")) {
                criteria.andAppIdIn(Arrays.asList(condition.getAppId().split(",")));
            } else {
                criteria.andAppIdEqualTo(condition.getAppId());
            }
        }
        if (condition.getClusterId() != null) {
            if (condition.getClusterId().contains(",")) {
                criteria.andClusterIdIn(Arrays.asList(condition.getClusterId().split(",")));
            } else {
                criteria.andClusterIdEqualTo(condition.getClusterId());
            }
        }
        if (condition.getNamespaceId() != null) {
            if (condition.getNamespaceId().contains(",")) {
                criteria.andNamespaceIdIn(Arrays.asList(condition.getNamespaceId().split(",")));
            } else {
                criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
            }
        }
        if (condition.getStageId() != null) {
            if (condition.getStageId().contains(",")) {
                criteria.andStageIdIn(Arrays.asList(condition.getStageId().split(",")));
            } else {
                criteria.andStageIdEqualTo(condition.getStageId());
            }
        }
        if (StringUtils.isNotBlank(condition.getStatus())) {
            criteria.andStatusEqualTo(condition.getStatus());
        }
        if (condition.getVisit() != null) {
            criteria.andVisitEqualTo(condition.getVisit());
        }
        if (condition.getUpgrade() != null) {
            criteria.andUpgradeEqualTo(condition.getUpgrade());
        }
        return example;
    }

    private RtAppInstanceDO insertDate(RtAppInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtAppInstanceDO updateDate(RtAppInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
