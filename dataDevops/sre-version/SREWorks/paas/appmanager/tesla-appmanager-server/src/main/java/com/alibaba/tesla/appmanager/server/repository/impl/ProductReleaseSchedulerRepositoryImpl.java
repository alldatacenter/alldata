package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseSchedulerRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseSchedulerQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseSchedulerDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseSchedulerRepositoryImpl implements ProductReleaseSchedulerRepository {

    @Autowired
    private ProductReleaseSchedulerDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseSchedulerQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseSchedulerQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseSchedulerDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseSchedulerDO> selectByCondition(ProductReleaseSchedulerQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public ProductReleaseSchedulerDO getByCondition(ProductReleaseSchedulerQueryCondition condition) {
        List<ProductReleaseSchedulerDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple product release scheduler found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ProductReleaseSchedulerDO record, ProductReleaseSchedulerQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ProductReleaseSchedulerDOExample buildExample(ProductReleaseSchedulerQueryCondition condition) {
        ProductReleaseSchedulerDOExample example = new ProductReleaseSchedulerDOExample();
        ProductReleaseSchedulerDOExample.Criteria criteria = example.createCriteria();
        if (condition.getId() != null && condition.getId() > 0) {
            criteria.andIdEqualTo(condition.getId());
        }
        if (StringUtils.isNotBlank(condition.getProductId())) {
            criteria.andProductIdEqualTo(condition.getProductId());
        }
        if (StringUtils.isNotBlank(condition.getReleaseId())) {
            criteria.andReleaseIdEqualTo(condition.getReleaseId());
        }
        return example;
    }

    private ProductReleaseSchedulerDO insertDate(ProductReleaseSchedulerDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ProductReleaseSchedulerDO updateDate(ProductReleaseSchedulerDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
