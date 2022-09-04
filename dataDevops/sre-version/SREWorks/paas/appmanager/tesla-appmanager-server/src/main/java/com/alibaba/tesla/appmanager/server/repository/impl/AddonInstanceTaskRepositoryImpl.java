package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.AddonInstanceTaskMapper;
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
public class AddonInstanceTaskRepositoryImpl implements AddonInstanceTaskRepository {

    @Autowired
    private AddonInstanceTaskMapper addonInstanceTaskMapper;

    @Override
    public long countByCondition(AddonInstanceTaskQueryCondition condition) {
        return addonInstanceTaskMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AddonInstanceTaskQueryCondition condition) {
        return addonInstanceTaskMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return addonInstanceTaskMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(AddonInstanceTaskDO record) {
        return addonInstanceTaskMapper.insertSelective(insertDate(record));
    }

    @Override
    public int insertOrUpdate(AddonInstanceTaskDO record) {
        return addonInstanceTaskMapper.insertOrUpdateSelective(insertDate(record));
    }

    @Override
    public List<AddonInstanceTaskDO> selectByCondition(AddonInstanceTaskQueryCondition condition) {
        List<AddonInstanceTaskDO> tasks = addonInstanceTaskMapper.selectByExample(buildExample(condition));
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
    public AddonInstanceTaskDO selectByPrimaryKey(Long id) {
        return addonInstanceTaskMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(AddonInstanceTaskDO record, AddonInstanceTaskQueryCondition condition) {
        return addonInstanceTaskMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(AddonInstanceTaskDO record) {
        return addonInstanceTaskMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private AddonInstanceTaskDOExample buildExample(AddonInstanceTaskQueryCondition condition) {
        AddonInstanceTaskDOExample example = new AddonInstanceTaskDOExample();
        AddonInstanceTaskDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getNamespaceId())) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (StringUtils.isNotBlank(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotBlank(condition.getAddonVersion())) {
            criteria.andAddonVersionEqualTo(condition.getAddonVersion());
        }
        if (StringUtils.isNotBlank(condition.getAddonName())) {
            criteria.andAddonNameEqualTo(condition.getAddonName());
        }
        if (condition.getTaskProcessId() != null && condition.getTaskProcessId() > 0) {
            criteria.andTaskProcessIdEqualTo(condition.getTaskProcessId());
        }
        if (condition.getTaskStatusList() != null && condition.getTaskStatusList().size() > 0) {
            criteria.andTaskStatusIn(condition.getTaskStatusList());
        }
        return example;
    }

    private AddonInstanceTaskDO insertDate(AddonInstanceTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private AddonInstanceTaskDO updateDate(AddonInstanceTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
