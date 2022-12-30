package cn.datax.service.data.market.mapping.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.market.api.entity.ApiLogEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

@Mapper
public interface ApiLogDao extends BaseDao<ApiLogEntity> {

    @Override
    ApiLogEntity selectById(Serializable id);

    @Override
    <E extends IPage<ApiLogEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<ApiLogEntity> queryWrapper);
}
