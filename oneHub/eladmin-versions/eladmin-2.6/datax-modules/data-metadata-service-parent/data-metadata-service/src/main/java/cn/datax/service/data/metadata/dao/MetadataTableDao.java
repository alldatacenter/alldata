package cn.datax.service.data.metadata.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 数据库表信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper
public interface MetadataTableDao extends BaseDao<MetadataTableEntity> {

    <E extends IPage<MetadataTableEntity>> E selectPageWithAuth(E page, @Param(Constants.WRAPPER) Wrapper<MetadataTableEntity> queryWrapper, @Param("roles") List<String> roles);
}
