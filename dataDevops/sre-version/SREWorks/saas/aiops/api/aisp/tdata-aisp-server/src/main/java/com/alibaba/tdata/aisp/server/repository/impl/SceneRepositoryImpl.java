package com.alibaba.tdata.aisp.server.repository.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.tdata.aisp.server.common.condition.SceneQueryCondition;
import com.alibaba.tdata.aisp.server.repository.SceneRepository;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDO;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDOExample;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDOExample.Criteria;
import com.alibaba.tdata.aisp.server.repository.mapper.SceneConfigDOMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: SceneRepositoryImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Component
public class SceneRepositoryImpl implements SceneRepository {
    @Resource
    private SceneConfigDOMapper sceneConfigDOMapper;

    @Override
    public int insert(SceneConfigDO configDO) {
        Date date = new Date();
        configDO.setGmtCreate(date);
        configDO.setGmtModified(date);
        return sceneConfigDOMapper.insert(configDO);
    }

    @Override
    public int deleteById(String sceneCode) {
        return sceneConfigDOMapper.deleteByPrimaryKey(sceneCode);
    }

    @Override
    public List<SceneConfigDO> queryByRowBounds(SceneQueryCondition condition, Integer limit, Integer skip) {
        SceneConfigDOExample example = convert(condition);
        example.setOrderByClause("`gmt_create` DESC");
        RowBounds rowBounds = new RowBounds(skip, limit);
        return sceneConfigDOMapper.selectByExampleWithBLOBsWithRowbounds(example, rowBounds);
    }

    @Override
    public SceneConfigDO queryById(String sceneCode) {
        return sceneConfigDOMapper.selectByPrimaryKey(sceneCode);
    }

    @Override
    public int updateById(SceneConfigDO sceneConfigDO) {
        return sceneConfigDOMapper.updateByPrimaryKeyWithBLOBs(sceneConfigDO);
    }

    private SceneConfigDOExample convert(SceneQueryCondition condition) {
        SceneConfigDOExample example = new SceneConfigDOExample();
        Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(condition.getSceneCode())){
            criteria.andSceneCodeEqualTo(condition.getSceneCode());
        }
        if (!StringUtils.isEmpty(condition.getSceneCodeLike())){
            criteria.andSceneCodeLike("%".concat(condition.getSceneCodeLike()).concat("%"));
        }
        if (!StringUtils.isEmpty(condition.getSceneName())){
            criteria.andSceneNameEqualTo(condition.getSceneName());
        }
        if (!StringUtils.isEmpty(condition.getSceneNameLike())){
            criteria.andSceneCodeLike("%".concat(condition.getSceneNameLike()).concat("%"));
        }
        if (!CollectionUtils.isEmpty(condition.getProductList())){
            for (String s : condition.getProductList()) {
                criteria.andProductNameLike("%".concat(s).concat("%"));
            }
        }
        if (!StringUtils.isEmpty(condition.getOwners())){
            String[] split = condition.getOwners().split(",");
            for (String user : split) {
                criteria.andOwnersLike("%".concat(user).concat("%"));
            }
        }
        return example;
    }
}
