package cn.datax.service.codegen.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.codegen.api.entity.GenTableEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 代码生成信息表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @since 2020-05-19
 */
@Mapper
public interface GenTableDao extends BaseDao<GenTableEntity> {

    @Override
    GenTableEntity selectById(Serializable id);

    @Override
    <E extends IPage<GenTableEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<GenTableEntity> queryWrapper);
}
