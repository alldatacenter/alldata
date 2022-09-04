package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseTaskTagRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskTagDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseTaskTagDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseTaskTagRepositoryImpl implements ProductReleaseTaskTagRepository {

    @Autowired
    private ProductReleaseTaskTagDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseTaskTagQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseTaskTagQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseTaskTagDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseTaskTagDO> selectByCondition(ProductReleaseTaskTagQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    private ProductReleaseTaskTagDOExample buildExample(ProductReleaseTaskTagQueryCondition condition) {
        ProductReleaseTaskTagDOExample example = new ProductReleaseTaskTagDOExample();
        ProductReleaseTaskTagDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getTaskId())) {
            criteria.andTaskIdEqualTo(condition.getTaskId());
        }
        if (CollectionUtils.isNotEmpty(condition.getTagList())) {
            criteria.andTagIn(condition.getTagList());
        }
        return example;
    }

    private ProductReleaseTaskTagDO insertDate(ProductReleaseTaskTagDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }
}
