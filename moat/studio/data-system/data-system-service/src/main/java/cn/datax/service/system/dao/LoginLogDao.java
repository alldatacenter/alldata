package cn.datax.service.system.dao;

import cn.datax.common.base.BaseDao;
import cn.datax.service.system.api.entity.LoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 登录日志信息表 Mapper 接口
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Mapper
public interface LoginLogDao extends BaseDao<LoginLogEntity> {

}
