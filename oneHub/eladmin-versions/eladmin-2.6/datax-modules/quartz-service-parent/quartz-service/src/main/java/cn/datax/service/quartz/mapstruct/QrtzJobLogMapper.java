package cn.datax.service.quartz.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.quartz.api.dto.QrtzJobLogDto;
import cn.datax.service.quartz.api.entity.QrtzJobLogEntity;
import cn.datax.service.quartz.api.vo.QrtzJobLogVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 定时任务日志信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Mapper(componentModel = "spring")
public interface QrtzJobLogMapper extends EntityMapper<QrtzJobLogDto, QrtzJobLogEntity, QrtzJobLogVo> {

}
