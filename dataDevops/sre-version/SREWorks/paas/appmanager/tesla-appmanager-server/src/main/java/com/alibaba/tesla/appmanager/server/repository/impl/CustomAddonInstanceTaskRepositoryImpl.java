package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.CustomAddonInstanceTaskDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @ClassName:CustomAddonInstanceTaskRepositoryImpl
 * @DATE: 2020-11-25
 * @Description:
 **/
@Service
public class CustomAddonInstanceTaskRepositoryImpl implements CustomAddonInstanceTaskRepository {
    @Autowired
    private CustomAddonInstanceTaskDOMapper customAddonInstanceTaskDOMapper;

    @Override
    public long countByCondition(AddonInstanceTaskQueryCondition condition) {
        return customAddonInstanceTaskDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(AddonInstanceTaskQueryCondition condition) {
        return customAddonInstanceTaskDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return customAddonInstanceTaskDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(CustomAddonInstanceTaskDO record) {
        return customAddonInstanceTaskDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public int insertOrUpdate(CustomAddonInstanceTaskDO record) {
        return customAddonInstanceTaskDOMapper.insertOrUpdateSelective(insertDate(record));
    }

    @Override
    public List<CustomAddonInstanceTaskDO> selectByCondition(AddonInstanceTaskQueryCondition condition) {
        List<CustomAddonInstanceTaskDO> tasks = customAddonInstanceTaskDOMapper.selectByExampleWithBLOBs(buildExample(condition));
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
    public CustomAddonInstanceTaskDO selectByPrimaryKey(Long id) {
        return customAddonInstanceTaskDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(CustomAddonInstanceTaskDO record, AddonInstanceTaskQueryCondition condition) {
        return customAddonInstanceTaskDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(CustomAddonInstanceTaskDO record) {
        return customAddonInstanceTaskDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private CustomAddonInstanceTaskDOExample buildExample(AddonInstanceTaskQueryCondition condition) {
        CustomAddonInstanceTaskDOExample example = new CustomAddonInstanceTaskDOExample();
        CustomAddonInstanceTaskDOExample.Criteria criteria = example.createCriteria();
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

    private CustomAddonInstanceTaskDO insertDate(CustomAddonInstanceTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private CustomAddonInstanceTaskDO updateDate(CustomAddonInstanceTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
