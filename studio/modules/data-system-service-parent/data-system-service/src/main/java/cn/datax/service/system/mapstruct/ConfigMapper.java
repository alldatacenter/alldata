package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.ConfigDto;
import cn.datax.service.system.api.entity.ConfigEntity;
import cn.datax.service.system.api.vo.ConfigVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 系统参数配置信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @date 2022-05-19
 */
@Mapper(componentModel = "spring")
public interface ConfigMapper extends EntityMapper<ConfigDto, ConfigEntity, ConfigVo> {

}
