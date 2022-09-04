package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.NamespaceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.NamespaceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.NamespaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class NamespaceRepositoryImpl implements NamespaceRepository {

    @Resource
    private NamespaceMapper namespaceMapper;

    @Override
    public long countByCondition(NamespaceQueryCondition condition) {
        return namespaceMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(NamespaceQueryCondition condition) {
        return namespaceMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(NamespaceDO record) {
        return namespaceMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<NamespaceDO> selectByCondition(NamespaceQueryCondition condition) {
        NamespaceDOExample example = buildExample(condition);
        condition.doPagination();
        return namespaceMapper.selectByExample(example);
    }

    @Override
    public int updateByCondition(NamespaceDO record, NamespaceQueryCondition condition) {
        return namespaceMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private NamespaceDOExample buildExample(NamespaceQueryCondition condition) {
        NamespaceDOExample example = new NamespaceDOExample();
        NamespaceDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getNamespaceId())) {
            criteria.andNamespaceIdEqualTo(condition.getNamespaceId());
        }
        if (StringUtils.isNotBlank(condition.getNamespaceName())) {
            criteria.andNamespaceNameEqualTo(condition.getNamespaceName());
        }
        if (StringUtils.isNotBlank(condition.getNamespaceCreator())) {
            criteria.andNamespaceCreatorEqualTo(condition.getNamespaceCreator());
        }
        if (StringUtils.isNotBlank(condition.getNamespaceModifier())) {
            criteria.andNamespaceModifierEqualTo(condition.getNamespaceModifier());
        }
        return example;
    }

    private NamespaceDO insertDate(NamespaceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private NamespaceDO updateDate(NamespaceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
