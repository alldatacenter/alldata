package cn.datax.service.data.standard.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.standard.api.dto.ContrastDto;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.vo.ContrastStatisticVo;
import cn.datax.service.data.standard.api.vo.ContrastVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 对照表信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Mapper(componentModel = "spring")
public interface ContrastMapper extends EntityMapper<ContrastDto, ContrastEntity, ContrastVo> {

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    ContrastStatisticVo toSVO(ContrastEntity e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<ContrastStatisticVo> toSVO(List<ContrastEntity> es);
}
