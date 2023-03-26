package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.LoginLogDto;
import cn.datax.service.system.api.entity.LoginLogEntity;
import cn.datax.service.system.api.vo.LoginLogVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 登录日志信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Mapper(componentModel = "spring")
public interface LoginLogMapper extends EntityMapper<LoginLogDto, LoginLogEntity, LoginLogVo> {

}
