package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ProductReleaseTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ProductReleaseTaskDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductReleaseTaskRepositoryImpl implements ProductReleaseTaskRepository {

    @Autowired
    private ProductReleaseTaskDOMapper mapper;

    @Override
    public long countByCondition(ProductReleaseTaskQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ProductReleaseTaskQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ProductReleaseTaskDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ProductReleaseTaskDO> selectByCondition(ProductReleaseTaskQueryCondition condition) {
        String productId = condition.getProductId();
        String releaseId = condition.getReleaseId();
        String taskId = condition.getTaskId();
        List<String> tags = condition.getTags();
        ProductReleaseTaskDOExample example = buildExample(condition);
        condition.doPagination();
        if (!CollectionUtils.isEmpty(tags)) {
            return mapper.selectByTags(productId, releaseId, taskId, tags, tags.size(), example);
        }
        return mapper.selectByExample(example);
    }

    @Override
    public ProductReleaseTaskDO getByCondition(ProductReleaseTaskQueryCondition condition) {
        List<ProductReleaseTaskDO> results = selectByCondition(condition);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple product release task found with condition %s",
                            JSONObject.toJSONString(condition)));
        }
    }

    @Override
    public int updateByCondition(ProductReleaseTaskDO record, ProductReleaseTaskQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ProductReleaseTaskDOExample buildExample(ProductReleaseTaskQueryCondition condition) {
        ProductReleaseTaskDOExample example = new ProductReleaseTaskDOExample();
        ProductReleaseTaskDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getProductId())) {
            criteria.andProductIdEqualTo(condition.getProductId());
        }
        if (StringUtils.isNotBlank(condition.getReleaseId())) {
            criteria.andReleaseIdEqualTo(condition.getReleaseId());
        }
        if (StringUtils.isNotBlank(condition.getTaskId())) {
            criteria.andTaskIdEqualTo(condition.getTaskId());
        }
        if (CollectionUtils.isNotEmpty(condition.getStatus())) {
            criteria.andStatusIn(condition.getStatus());
        }
        return example;
    }

    private ProductReleaseTaskDO insertDate(ProductReleaseTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ProductReleaseTaskDO updateDate(ProductReleaseTaskDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
