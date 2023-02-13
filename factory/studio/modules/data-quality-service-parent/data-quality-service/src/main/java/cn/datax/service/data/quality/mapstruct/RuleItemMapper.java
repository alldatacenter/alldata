package cn.datax.service.data.quality.mapstruct;

import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import cn.datax.service.data.quality.api.vo.RuleItemVo;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * <p>
 * 规则核查项信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Mapper(componentModel = "spring")
public interface RuleItemMapper {

    /**
     * 将源对象转换为VO对象
     * @param e
     * @return D
     */
    RuleItemVo toVO(RuleItemEntity e);

    /**
     * 将源对象集合转换为VO对象集合
     * @param es
     * @return List<D>
     */
    List<RuleItemVo> toVO(List<RuleItemEntity> es);
}
