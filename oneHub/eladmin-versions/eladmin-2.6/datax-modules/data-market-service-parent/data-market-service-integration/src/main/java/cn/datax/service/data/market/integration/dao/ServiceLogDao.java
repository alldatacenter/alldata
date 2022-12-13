package cn.datax.service.data.market.integration.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.market.api.entity.ServiceLogEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 服务集成调用日志表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Mapper
public interface ServiceLogDao extends BaseDao<ServiceLogEntity> {

    @Override
    ServiceLogEntity selectById(Serializable id);

    @Override
    <E extends IPage<ServiceLogEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<ServiceLogEntity> queryWrapper);
}
