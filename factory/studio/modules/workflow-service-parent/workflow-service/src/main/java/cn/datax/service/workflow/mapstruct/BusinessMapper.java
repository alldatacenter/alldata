package cn.datax.service.workflow.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.workflow.api.dto.BusinessDto;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import cn.datax.service.workflow.api.vo.BusinessVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 业务流程配置表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Mapper(componentModel = "spring")
public interface BusinessMapper extends EntityMapper<BusinessDto, BusinessEntity, BusinessVo> {

}
