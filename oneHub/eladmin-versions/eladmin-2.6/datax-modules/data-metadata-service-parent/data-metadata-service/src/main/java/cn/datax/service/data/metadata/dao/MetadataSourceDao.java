package cn.datax.service.data.metadata.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 数据源信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper
public interface MetadataSourceDao extends BaseDao<MetadataSourceEntity> {

    @Override
    MetadataSourceEntity selectById(Serializable id);

    @Override
    List<MetadataSourceEntity> selectList(@Param(Constants.WRAPPER) Wrapper<MetadataSourceEntity> queryWrapper);

    <E extends IPage<MetadataSourceEntity>> E selectPageWithAuth(E page, @Param(Constants.WRAPPER) Wrapper<MetadataSourceEntity> queryWrapper, @Param("roles") List<String> roles);
}
