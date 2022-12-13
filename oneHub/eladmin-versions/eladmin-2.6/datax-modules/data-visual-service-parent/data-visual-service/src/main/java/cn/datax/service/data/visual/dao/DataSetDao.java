package cn.datax.service.data.visual.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 数据集信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Mapper
public interface DataSetDao extends BaseDao<DataSetEntity> {

    @Override
    DataSetEntity selectById(Serializable id);

    @Override
    <E extends IPage<DataSetEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<DataSetEntity> queryWrapper);
}
