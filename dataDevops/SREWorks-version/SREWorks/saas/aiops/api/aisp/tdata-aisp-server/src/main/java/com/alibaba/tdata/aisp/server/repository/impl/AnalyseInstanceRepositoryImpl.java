package com.alibaba.tdata.aisp.server.repository.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.tdata.aisp.server.common.condition.InstanceQueryCondition;
import com.alibaba.tdata.aisp.server.repository.AnalyseInstanceRepository;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDOExample;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDOExample.Criteria;
import com.alibaba.tdata.aisp.server.repository.mapper.InstanceDOMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @ClassName: AnalyseInstanceRepositoryImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Component
public class AnalyseInstanceRepositoryImpl implements AnalyseInstanceRepository {
    @Resource
    private InstanceDOMapper instanceDOMapper;

    /**
     * @param instanceDO
     * @return
     */
    @Override
    public int insert(InstanceDO instanceDO) {
        Date date = new Date();
        instanceDO.setGmtCreate(date);
        instanceDO.setGmtModified(date);
        return instanceDOMapper.insert(instanceDO);
    }

    /**
     * @param instanceCode
     * @return
     */
    @Override
    public InstanceDO queryById(String instanceCode) {
        return instanceDOMapper.selectByPrimaryKey(instanceCode);
    }

    /**
     * @param condition
     * @return
     */
    @Override
    public List<InstanceDO> queryByCondition(InstanceQueryCondition condition) {
        InstanceDOExample example = convert(condition);
        return instanceDOMapper.selectByExampleWithBLOBs(example);
    }


    /**
     * @param instanceCode
     * @return
     */
    @Override
    public int deleteById(String instanceCode) {
        return instanceDOMapper.deleteByPrimaryKey(instanceCode);
    }

    @Override
    public int updateById(InstanceDO instanceDO) {
        return instanceDOMapper.updateByPrimaryKeyWithBLOBs(instanceDO);
    }

    @Override
    public List<InstanceDO> queryByRowBounds(InstanceQueryCondition condition, Integer limit, Integer skip) {
        InstanceDOExample example = convert(condition);
        example.setOrderByClause("`gmt_create` DESC");
        RowBounds rowBounds = new RowBounds(skip, limit);
        return instanceDOMapper.selectByExampleWithBLOBsWithRowbounds(example, rowBounds);
    }

    private InstanceDOExample convert(InstanceQueryCondition condition) {
        InstanceDOExample example = new InstanceDOExample();
        Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(condition.getInstanceCode())){
            criteria.andInstanceCodeEqualTo(condition.getInstanceCode());
        }
        if (!StringUtils.isEmpty(condition.getSceneCode())){
            criteria.andSceneCodeEqualTo(condition.getSceneCode());
        }
        if(!StringUtils.isEmpty(condition.getDetectorCode())){
            criteria.andDetectorCodeEqualTo(condition.getDetectorCode());
        }
        if (!StringUtils.isEmpty(condition.getEntityId())){
            criteria.andEntityIdEqualTo(condition.getEntityId());
        }
        return example;
    }
}
