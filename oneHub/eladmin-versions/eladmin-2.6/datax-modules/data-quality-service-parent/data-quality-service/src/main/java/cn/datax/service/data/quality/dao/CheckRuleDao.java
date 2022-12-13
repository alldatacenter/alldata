package cn.datax.service.data.quality.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 核查规则信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper
public interface CheckRuleDao extends BaseDao<CheckRuleEntity> {

    @Override
    CheckRuleEntity selectById(Serializable id);

    @Override
    List<CheckRuleEntity> selectList(@Param(Constants.WRAPPER) Wrapper<CheckRuleEntity> queryWrapper);

    @Override
    <E extends IPage<CheckRuleEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<CheckRuleEntity> queryWrapper);
}
