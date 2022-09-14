package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.RtComponentInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.RtComponentInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDOExample;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.mapper.RtComponentInstanceDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RtComponentInstanceRepositoryImpl implements RtComponentInstanceRepository {

    @Autowired
    private RtComponentInstanceHistoryRepository historyRepository;

    @Autowired
    private RtComponentInstanceDOMapper mapper;

    @Autowired
    private SystemProperties systemProperties;

    @Override
    public long countByCondition(RtComponentInstanceQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(RtComponentInstanceQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(RtComponentInstanceDO record) {
        int inserted = mapper.insertSelective(insertDate(record));
//        historyRepository.insert(RtComponentInstanceHistoryDO.builder()
//                .componentInstanceId(record.getComponentInstanceId())
//                .appInstanceId(record.getAppInstanceId())
//                .status(record.getStatus())
//                .conditions(record.getConditions())
//                .version(record.getVersion())
//                .build());
        return inserted;
    }

    @Override
    public List<RtComponentInstanceDO> selectByCondition(RtComponentInstanceQueryCondition condition) {
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
    public RtComponentInstanceDO getByCondition(RtComponentInstanceQueryCondition condition) {
        List<RtComponentInstanceDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple component instance found with query condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(RtComponentInstanceDO record, RtComponentInstanceQueryCondition condition) {
        int updated = mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
//        RtComponentInstanceHistoryQueryCondition historyCondition = RtComponentInstanceHistoryQueryCondition.builder()
//                .componentInstanceId(condition.getComponentInstanceId())
//                .build();
//        RtComponentInstanceHistoryDO latestHistoryRecord = historyRepository.getLatestByCondition(historyCondition);
//        if (latestHistoryRecord == null
//                || !record.getVersion().equals(latestHistoryRecord.getVersion())
//                || !record.getStatus().equals(latestHistoryRecord.getStatus())
//                || !record.getConditions().equals(latestHistoryRecord.getConditions())) {
//            historyRepository.insert(RtComponentInstanceHistoryDO.builder()
//                    .componentInstanceId(record.getComponentInstanceId())
//                    .appInstanceId(record.getAppInstanceId())
//                    .status(record.getStatus())
//                    .conditions(record.getConditions())
//                    .version(record.getVersion())
//                    .build());
//            int cleaned = historyRepository.deleteExpiredRecords(record.getComponentInstanceId(),
//                    systemProperties.getInstanceHistoryKeepDays());
//            if (cleaned > 0) {
//                log.info("action=deleteExpiredComponentInstanceHistory|componentInstanceId={}|cleaned={}",
//                        record.getComponentInstanceId(), cleaned);
//            }
//        }
        return updated;
    }

    private RtComponentInstanceDOExample buildExample(RtComponentInstanceQueryCondition condition) {
        RtComponentInstanceDOExample example = new RtComponentInstanceDOExample();
        RtComponentInstanceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAppInstanceId())) {
            criteria.andAppInstanceIdEqualTo(condition.getAppInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getComponentInstanceId())) {
            criteria.andComponentInstanceIdEqualTo(condition.getComponentInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getAppIdStartsWith())) {
            if (condition.getAppIdStartsWith().startsWith("!")) {
                criteria.andAppIdNotLike(String.format("%s%%", condition.getAppIdStartsWith().substring(1).trim()));
            } else {
                criteria.andAppIdLike(String.format("%s%%", condition.getAppIdStartsWith().trim()));
            }
        }
        if (StringUtils.isNotBlank(condition.getComponentType())) {
            criteria.andComponentTypeEqualTo(condition.getComponentType());
        }
        if (StringUtils.isNotBlank(condition.getComponentName())) {
            criteria.andComponentNameEqualTo(condition.getComponentName());
        }
        if (StringUtils.isNotBlank(condition.getComponentNameStartsWith())) {
            if (condition.getComponentNameStartsWith().startsWith("!")) {
                criteria.andComponentNameNotLike(String.format("%s%%",
                        condition.getComponentNameStartsWith().substring(1).trim()));
            } else {
                criteria.andComponentNameLike(String.format("%s%%", condition.getComponentNameStartsWith().trim()));
            }
        }
        if (condition.getClusterId() != null) {
            criteria.andClusterIdEqualTo(condition.getClusterId());
        }
        if (condition.getNamespaceId() != null) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (condition.getStageId() != null) {
            criteria.andStageIdEqualTo(condition.getStageId());
        }
        if (StringUtils.isNotBlank(condition.getStatus())) {
            criteria.andStatusEqualTo(condition.getStatus());
        }
        if (CollectionUtils.isNotEmpty(condition.getStatusList())) {
            criteria.andStatusIn(condition.getStatusList());
        }
        if (StringUtils.isNotBlank(condition.getWatchKind())) {
            criteria.andWatchKindEqualTo(condition.getWatchKind());
        }
        if (condition.getTimesGreaterThan() != null) {
            if (condition.getTimesLessThan() == null || condition.getTimesLessThan() == 0) {
                criteria.andTimesGreaterThan(condition.getTimesGreaterThan());
            } else {
                if (condition.getTimesGreaterThan() > condition.getTimesLessThan()) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("invalid query condition, timesGreaterThan %d must less than timesLessThan %d",
                                    condition.getTimesGreaterThan(), condition.getTimesLessThan()));
                }
                criteria.andTimesBetween(condition.getTimesGreaterThan(), condition.getTimesLessThan());
            }
        }
        return example;
    }

    private RtComponentInstanceDO insertDate(RtComponentInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private RtComponentInstanceDO updateDate(RtComponentInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
