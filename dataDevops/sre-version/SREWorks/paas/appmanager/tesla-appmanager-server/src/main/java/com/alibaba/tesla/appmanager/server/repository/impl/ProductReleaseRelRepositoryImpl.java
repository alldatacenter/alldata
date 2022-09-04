package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseRelRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseRelDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseRelRepositoryImpl implements ProductReleaseRelRepository {

    @Autowired
    private ProductReleaseRelDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseRelQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseRelQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseRelDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseRelDO> selectByCondition(ProductReleaseRelQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public ProductReleaseRelDO getByCondition(ProductReleaseRelQueryCondition condition) {
        List<ProductReleaseRelDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple product release rel found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ProductReleaseRelDO record, ProductReleaseRelQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ProductReleaseRelDOExample buildExample(ProductReleaseRelQueryCondition condition) {
        ProductReleaseRelDOExample example = new ProductReleaseRelDOExample();
        ProductReleaseRelDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getProductId())) {
            criteria.andProductIdEqualTo(condition.getProductId());
        }
        if (StringUtils.isNotBlank(condition.getReleaseId())) {
            criteria.andReleaseIdEqualTo(condition.getReleaseId());
        }
        if (StringUtils.isNotBlank(condition.getAppPackageTag())) {
            criteria.andAppPackageTagEqualTo(condition.getAppPackageTag());
        }
        return example;
    }

    private ProductReleaseRelDO insertDate(ProductReleaseRelDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ProductReleaseRelDO updateDate(ProductReleaseRelDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
