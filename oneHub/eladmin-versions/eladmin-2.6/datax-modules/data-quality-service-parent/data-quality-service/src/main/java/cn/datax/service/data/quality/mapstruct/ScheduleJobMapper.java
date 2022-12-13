package cn.datax.service.data.quality.mapstruct;

import cn.datax.service.data.quality.api.entity.ScheduleJobEntity;
import cn.datax.service.data.quality.api.vo.ScheduleJobVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 数据质量监控任务信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Mapper(componentModel = "spring")
public interface ScheduleJobMapper {

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    ScheduleJobVo toVO(ScheduleJobEntity e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<ScheduleJobVo> toVO(List<ScheduleJobEntity> es);
}
