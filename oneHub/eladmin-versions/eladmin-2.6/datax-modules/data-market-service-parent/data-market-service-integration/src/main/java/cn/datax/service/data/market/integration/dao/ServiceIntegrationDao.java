package cn.datax.service.data.market.integration.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.market.api.entity.ServiceIntegrationEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 服务集成表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-08-20
 */
@Mapper
public interface ServiceIntegrationDao extends BaseDao<ServiceIntegrationEntity> {

    @Override
    ServiceIntegrationEntity selectById(Serializable id);

    @Override
    <E extends IPage<ServiceIntegrationEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<ServiceIntegrationEntity> queryWrapper);
}
