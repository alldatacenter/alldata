package cn.datax.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BaseDao<T> extends BaseMapper<T> {

    List<T> selectListDataScope(@Param("ew") Wrapper<T> queryWrapper, @Param("dataScope") DataScope dataScope);

    IPage<T> selectPageDataScope(IPage<T> page, @Param("ew") Wrapper<T> queryWrapper, @Param("dataScope") DataScope dataScope);
}
