package cn.datax.service.data.quality.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.quality.api.dto.CheckRuleDto;
import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.vo.CheckRuleVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 核查规则信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper(componentModel = "spring")
public interface CheckRuleMapper extends EntityMapper<CheckRuleDto, CheckRuleEntity, CheckRuleVo> {

}
