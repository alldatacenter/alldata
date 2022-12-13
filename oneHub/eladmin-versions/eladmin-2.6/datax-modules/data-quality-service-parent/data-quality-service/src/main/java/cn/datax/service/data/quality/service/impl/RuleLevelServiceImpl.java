package cn.datax.service.data.quality.service.impl;

import cn.datax.service.data.quality.api.entity.RuleLevelEntity;
import cn.datax.service.data.quality.service.RuleLevelService;
import cn.datax.service.data.quality.mapstruct.RuleLevelMapper;
import cn.datax.service.data.quality.dao.RuleLevelDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 规则级别信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RuleLevelServiceImpl extends BaseServiceImpl<RuleLevelDao, RuleLevelEntity> implements RuleLevelService {

    @Autowired
    private RuleLevelDao ruleLevelDao;

    @Autowired
    private RuleLevelMapper ruleLevelMapper;

    @Override
    public RuleLevelEntity getRuleLevelById(String id) {
        RuleLevelEntity ruleLevelEntity = super.getById(id);
        return ruleLevelEntity;
    }
}
