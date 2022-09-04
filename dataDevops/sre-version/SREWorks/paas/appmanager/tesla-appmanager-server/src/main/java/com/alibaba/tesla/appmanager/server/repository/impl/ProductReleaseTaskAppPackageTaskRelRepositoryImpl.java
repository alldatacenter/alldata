package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseTaskAppPackageTaskRelRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskAppPackageTaskRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseTaskAppPackageTaskRelDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseTaskAppPackageTaskRelRepositoryImpl implements ProductReleaseTaskAppPackageTaskRelRepository {

    @Autowired
    private ProductReleaseTaskAppPackageTaskRelDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseTaskAppPackageTaskRelDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseTaskAppPackageTaskRelDO> selectByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    private ProductReleaseTaskAppPackageTaskRelDOExample buildExample(ProductReleaseTaskAppPackageTaskRelQueryCondition condition) {
        ProductReleaseTaskAppPackageTaskRelDOExample example = new ProductReleaseTaskAppPackageTaskRelDOExample();
        ProductReleaseTaskAppPackageTaskRelDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getTaskId())) {
            criteria.andTaskIdEqualTo(condition.getTaskId());
        }
        if (condition.getAppPackageTaskId() != null && condition.getAppPackageTaskId() > 0) {
            criteria.andAppPackageTaskIdEqualTo(condition.getAppPackageTaskId());
        }
        return example;
    }

    private ProductReleaseTaskAppPackageTaskRelDO insertDate(ProductReleaseTaskAppPackageTaskRelDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }
}
