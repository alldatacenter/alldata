package com.alibaba.tdata.aisp.server.repository.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.tdata.aisp.server.common.condition.DetectorQueryCondition;
import com.alibaba.tdata.aisp.server.repository.DetectorRepository;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDO;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDOExample;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDOExample.Criteria;
import com.alibaba.tdata.aisp.server.repository.mapper.DetectorConfigDOMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @ClassName: DetectorRepositoryImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Component
public class DetectorRepositoryImpl implements DetectorRepository {
    @Resource
    private DetectorConfigDOMapper detectorConfigDOMapper;

    /**
     * @param configDO
     * @return
     */
    @Override
    public int insert(DetectorConfigDO configDO) {
        Date date = new Date();
        configDO.setGmtCreate(date);
        configDO.setGmtModified(date);
        return detectorConfigDOMapper.insert(configDO);
    }

    @Override
    public DetectorConfigDO queryById(String detectorCode) {
        return detectorConfigDOMapper.selectByPrimaryKey(detectorCode);
    }

    /**
     * @param detectorCode
     * @return
     */
    @Override
    public int delete(String detectorCode) {
        return detectorConfigDOMapper.deleteByPrimaryKey(detectorCode);
    }

    @Override
    public int updateById(DetectorConfigDO configDO) {
        updateTime(configDO);
        return detectorConfigDOMapper.updateByPrimaryKey(configDO);
    }

    @Override
    public List<DetectorConfigDO> queryWithRowBounds(DetectorQueryCondition condition, Integer limit, Integer skip) {
        DetectorConfigDOExample example = convert(condition);
        example.setOrderByClause("`gmt_create` DESC");
        RowBounds rowBounds = new RowBounds(skip, limit);
        return detectorConfigDOMapper.selectByExampleWithBLOBsWithRowbounds(example, rowBounds);
    }

    private void updateTime(DetectorConfigDO configDO) {
        Date date = new Date();
        configDO.setGmtModified(date);
    }

    private DetectorConfigDOExample convert(DetectorQueryCondition condition) {
        DetectorConfigDOExample example = new DetectorConfigDOExample();
        Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(condition.getDetectorCode())){
            criteria.andDetectorCodeEqualTo(condition.getDetectorCode());
        }
        return example;
    }
}
