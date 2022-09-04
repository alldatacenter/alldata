package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseAppRelRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseAppRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseAppRelDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseAppRelRepositoryImpl implements ProductReleaseAppRelRepository {

    @Autowired
    private ProductReleaseAppRelDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseAppRelQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseAppRelQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseAppRelDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseAppRelDO> selectByCondition(ProductReleaseAppRelQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public ProductReleaseAppRelDO getByCondition(ProductReleaseAppRelQueryCondition condition) {
        List<ProductReleaseAppRelDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple product release app rel found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ProductReleaseAppRelDO record, ProductReleaseAppRelQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ProductReleaseAppRelDOExample buildExample(ProductReleaseAppRelQueryCondition condition) {
        ProductReleaseAppRelDOExample example = new ProductReleaseAppRelDOExample();
        ProductReleaseAppRelDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getProductId())) {
            criteria.andProductIdEqualTo(condition.getProductId());
        }
        if (StringUtils.isNotBlank(condition.getReleaseId())) {
            criteria.andReleaseIdEqualTo(condition.getReleaseId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getTag())) {
            criteria.andTagEqualTo(condition.getTag());
        }
        return example;
    }

    private ProductReleaseAppRelDO insertDate(ProductReleaseAppRelDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ProductReleaseAppRelDO updateDate(ProductReleaseAppRelDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
