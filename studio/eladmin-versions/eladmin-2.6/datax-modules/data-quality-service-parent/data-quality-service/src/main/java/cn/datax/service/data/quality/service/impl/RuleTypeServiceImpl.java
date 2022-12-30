package cn.datax.service.data.quality.service.impl;

import cn.datax.service.data.quality.api.entity.RuleTypeEntity;
import cn.datax.service.data.quality.api.entity.RuleTypeReportEntity;
import cn.datax.service.data.quality.service.RuleTypeService;
import cn.datax.service.data.quality.mapstruct.RuleTypeMapper;
import cn.datax.service.data.quality.dao.RuleTypeDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 规则类型信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RuleTypeServiceImpl extends BaseServiceImpl<RuleTypeDao, RuleTypeEntity> implements RuleTypeService {

    @Autowired
    private RuleTypeDao ruleTypeDao;

    @Autowired
    private RuleTypeMapper ruleTypeMapper;

    @Override
    public RuleTypeEntity getRuleTypeById(String id) {
        RuleTypeEntity ruleTypeEntity = super.getById(id);
        return ruleTypeEntity;
    }

    @Override
    public List<RuleTypeReportEntity> getRuleTypeListForReport() {
        List<RuleTypeReportEntity> list = ruleTypeDao.selectListForReport();
        return list;
    }
}
