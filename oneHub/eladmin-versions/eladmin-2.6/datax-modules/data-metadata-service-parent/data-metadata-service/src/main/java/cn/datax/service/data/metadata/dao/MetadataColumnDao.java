package cn.datax.service.data.metadata.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 元数据信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper
public interface MetadataColumnDao extends BaseDao<MetadataColumnEntity> {

    <E extends IPage<MetadataColumnEntity>> E selectPageWithAuth(E page, @Param(Constants.WRAPPER) Wrapper<MetadataColumnEntity> queryWrapper, @Param("roles") List<String> roles);
}
