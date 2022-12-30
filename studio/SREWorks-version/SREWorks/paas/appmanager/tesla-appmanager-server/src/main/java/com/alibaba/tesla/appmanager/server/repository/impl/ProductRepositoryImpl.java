package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductRepositoryImpl implements ProductRepository {

    @Autowired
    private ProductDOMapper mapper;

    @Override
    public long countByCondition(ProductQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductDO> selectByCondition(ProductQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public ProductDO getByCondition(ProductQueryCondition condition) {
        List<ProductDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple product found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ProductDO record, ProductQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ProductDOExample buildExample(ProductQueryCondition condition) {
        ProductDOExample example = new ProductDOExample();
        ProductDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getProductId())) {
            criteria.andProductIdEqualTo(condition.getProductId());
        }
        if (StringUtils.isNotBlank(condition.getProductName())) {
            criteria.andProductNameEqualTo(condition.getProductName());
        }
        return example;
    }

    private ProductDO insertDate(ProductDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ProductDO updateDate(ProductDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
