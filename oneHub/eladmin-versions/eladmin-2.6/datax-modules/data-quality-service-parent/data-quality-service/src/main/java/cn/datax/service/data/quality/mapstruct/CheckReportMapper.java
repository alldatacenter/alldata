package cn.datax.service.data.quality.mapstruct;

import cn.datax.service.data.quality.api.entity.CheckReportEntity;
import cn.datax.service.data.quality.api.vo.CheckReportVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 核查报告信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper(componentModel = "spring")
public interface CheckReportMapper {

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    CheckReportVo toVO(CheckReportEntity e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<CheckReportVo> toVO(List<CheckReportEntity> es);
}
