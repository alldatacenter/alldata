package cn.datax.service.data.quality.service.impl;

import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import cn.datax.service.data.quality.service.RuleItemService;
import cn.datax.service.data.quality.mapstruct.RuleItemMapper;
import cn.datax.service.data.quality.dao.RuleItemDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 规则核查项信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RuleItemServiceImpl extends BaseServiceImpl<RuleItemDao, RuleItemEntity> implements RuleItemService {

    @Autowired
    private RuleItemDao ruleItemDao;

    @Autowired
    private RuleItemMapper ruleItemMapper;

    @Override
    public RuleItemEntity getRuleItemById(String id) {
        RuleItemEntity ruleItemEntity = super.getById(id);
        return ruleItemEntity;
    }
}
