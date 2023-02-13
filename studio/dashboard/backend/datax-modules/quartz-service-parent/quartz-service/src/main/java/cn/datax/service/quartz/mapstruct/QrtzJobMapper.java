package cn.datax.service.quartz.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.quartz.api.dto.QrtzJobDto;
import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.api.vo.QrtzJobVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 定时任务信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper(componentModel = "spring")
public interface QrtzJobMapper extends EntityMapper<QrtzJobDto, QrtzJobEntity, QrtzJobVo> {

}
