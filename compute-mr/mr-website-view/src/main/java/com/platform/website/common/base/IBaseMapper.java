package com.platform.website.common.base;

import java.util.List;
import org.apache.ibatis.session.RowBounds;

public interface IBaseMapper<T> {
	int deleteByPrimaryKey(Integer id);

    int insert(T record);

    int insertSelective(T record);

    T selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(T record);

    int updateByPrimaryKey(T record);
    List<T> getAllByPage(RowBounds rowBounds);
}

