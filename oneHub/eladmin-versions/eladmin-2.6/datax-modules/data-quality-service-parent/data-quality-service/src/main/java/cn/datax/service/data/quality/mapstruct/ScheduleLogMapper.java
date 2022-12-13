package cn.datax.service.data.quality.mapstruct;

import cn.datax.service.data.quality.api.entity.ScheduleLogEntity;
import cn.datax.service.data.quality.api.vo.ScheduleLogVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 数据质量监控任务日志信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-13
 */
@Mapper(componentModel = "spring")
public interface ScheduleLogMapper {

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    ScheduleLogVo toVO(ScheduleLogEntity e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<ScheduleLogVo> toVO(List<ScheduleLogEntity> es);
}
