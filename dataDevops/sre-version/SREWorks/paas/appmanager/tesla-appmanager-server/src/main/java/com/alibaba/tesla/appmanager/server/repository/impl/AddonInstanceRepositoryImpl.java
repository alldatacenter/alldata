package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AddonInstanceDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddonInstanceRepositoryImpl implements AddonInstanceRepository {

    @Autowired
    private AddonInstanceDOMapper addonInstanceDOMapper;

    @Override
    public long countByCondition(AddonInstanceQueryCondition condition) {
        return addonInstanceDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AddonInstanceQueryCondition condition) {
        return addonInstanceDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(AddonInstanceDO record) {
        return addonInstanceDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public AddonInstanceDO getByCondition(AddonInstanceQueryCondition condition) {
        List<AddonInstanceDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else if (records.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    "multiple addon instance found|%s", JSONObject.toJSONString(condition));
        } else {
            return records.get(0);
        }
    }

    @Override
    public List<AddonInstanceDO> selectByCondition(AddonInstanceQueryCondition condition) {
        List<AddonInstanceDO> tasks = addonInstanceDOMapper.selectByExample(buildExample(condition));
        Map<String, String> addonAttrs = condition.getAddonAttrs();
        if (addonAttrs == null || addonAttrs.size() == 0) {
            return tasks;
        }
        JSONObject addonAttrsJson = JSONObject.parseObject(JSONObject.toJSONString(addonAttrs));
        return tasks.stream()
                .filter(item -> JSONObject.parseObject(item.getAddonAttrs()).equals(addonAttrsJson))
                .collect(Collectors.toList());
    }

    @Override
    public int updateByPrimaryKey(AddonInstanceDO record) {
        return addonInstanceDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private AddonInstanceDOExample buildExample(AddonInstanceQueryCondition condition) {
        AddonInstanceDOExample example = new AddonInstanceDOExample();
        AddonInstanceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotBlank(condition.getAddonName())) {
            criteria.andAddonNameEqualTo(condition.getAddonName());
        }
        if (StringUtils.isNotBlank(condition.getAddonInstanceId())) {
            criteria.andAddonInstanceIdEqualTo(condition.getAddonInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getNamespaceId())) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        return example;
    }

    private AddonInstanceDO insertDate(AddonInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AddonInstanceDO updateDate(AddonInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
