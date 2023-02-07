package cn.datax.service.data.visual.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.data.visual.api.entity.ScreenEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

/**
 * <p>
 * 可视化酷屏配置信息表 Mapper 接口
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Mapper
public interface ScreenDao extends BaseDao<ScreenEntity> {

    @Override
    ScreenEntity selectById(Serializable id);

    @Override
    <E extends IPage<ScreenEntity>> E selectPage(E page, @Param(Constants.WRAPPER) Wrapper<ScreenEntity> queryWrapper);
}
