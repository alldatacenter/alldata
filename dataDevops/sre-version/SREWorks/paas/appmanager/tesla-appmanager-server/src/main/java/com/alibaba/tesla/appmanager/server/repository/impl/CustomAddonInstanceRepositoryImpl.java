package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.CustomAddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.CustomAddonInstanceDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @ClassName:CustomAddonInstanceRepositoryImpl
 * @DATE: 2020-11-25
 * @Description:
 **/
@Service
public class CustomAddonInstanceRepositoryImpl implements CustomAddonInstanceRepository {
    @Autowired
    private CustomAddonInstanceDOMapper customAddonInstanceDOMapper;

    @Override
    public long countByCondition(CustomAddonInstanceQueryCondition condition) {
        return customAddonInstanceDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(CustomAddonInstanceQueryCondition condition) {
        return customAddonInstanceDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return customAddonInstanceDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(CustomAddonInstanceDO record) {
        return customAddonInstanceDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public int insertOrUpdate(CustomAddonInstanceDO record) {
        return customAddonInstanceDOMapper.insertOrUpdateSelective(insertDate(record));
    }

    @Override
    public CustomAddonInstanceDO getByCondition(CustomAddonInstanceQueryCondition condition) {
        List<CustomAddonInstanceDO> records = selectByCondition(condition);
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
    public List<CustomAddonInstanceDO> selectByCondition(CustomAddonInstanceQueryCondition condition) {
        List<CustomAddonInstanceDO> tasks = customAddonInstanceDOMapper.selectByExampleWithBLOBs(buildExample(condition));
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
    public CustomAddonInstanceDO selectByPrimaryKey(Long id) {
        return customAddonInstanceDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(CustomAddonInstanceDO record, CustomAddonInstanceQueryCondition condition) {
        return customAddonInstanceDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(CustomAddonInstanceDO record) {
        return customAddonInstanceDOMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private CustomAddonInstanceDOExample buildExample(CustomAddonInstanceQueryCondition condition) {
        CustomAddonInstanceDOExample example = new CustomAddonInstanceDOExample();
        CustomAddonInstanceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getAddonId())) {
            criteria.andAddonIdEqualTo(condition.getAddonId());
        }
        if (StringUtils.isNotBlank(condition.getAddonVersion())) {
            criteria.andAddonVersionEqualTo(condition.getAddonVersion());
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

    private CustomAddonInstanceDO insertDate(CustomAddonInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private CustomAddonInstanceDO updateDate(CustomAddonInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
