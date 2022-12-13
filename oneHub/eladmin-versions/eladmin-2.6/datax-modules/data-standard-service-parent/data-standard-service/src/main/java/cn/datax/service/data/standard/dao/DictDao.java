package cn.datax.service.data.standard.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.standard.api.entity.DictEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 数据标准字典表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Mapper
public interface DictDao extends BaseDao<DictEntity> {

    @Override
    DictEntity selectById(Serializable id);

    @Override
    <E extends IPage<DictEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<DictEntity> queryWrapper);
}
