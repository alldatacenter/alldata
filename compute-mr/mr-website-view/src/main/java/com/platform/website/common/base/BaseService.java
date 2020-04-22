package com.platform.website.common.base;

public abstract   class BaseService<T> implements IBaseService<T> {
	public abstract IBaseMapper<T> getBaseMapper();

	@Override
	public int deleteByPrimaryKey(Integer id) {
		return this.getBaseMapper().deleteByPrimaryKey(id);
	}

	@Override
	public int insert(T record) {
		
		return this.getBaseMapper().insert(record);
	}

	@Override
	public int insertSelective(T record) {
		// TODO Auto-generated method stub
		return this.getBaseMapper().insertSelective(record);
	}

	@Override
	public T selectByPrimaryKey(Integer id) {
		// TODO Auto-generated method stub
		return this.getBaseMapper().selectByPrimaryKey(id);
	}

	@Override
	public int updateByPrimaryKeySelective(T record) {
		// TODO Auto-generated method stub
		return this.getBaseMapper().updateByPrimaryKeySelective(record);
	}

	@Override
	public int updateByPrimaryKey(T record) {
		// TODO Auto-generated method stub
		return this.getBaseMapper().updateByPrimaryKey(record);
	}
	
}

