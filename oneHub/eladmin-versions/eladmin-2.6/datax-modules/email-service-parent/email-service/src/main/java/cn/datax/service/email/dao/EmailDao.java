package cn.datax.service.email.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.email.api.entity.EmailEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailDao extends BaseDao<EmailEntity> {
}
