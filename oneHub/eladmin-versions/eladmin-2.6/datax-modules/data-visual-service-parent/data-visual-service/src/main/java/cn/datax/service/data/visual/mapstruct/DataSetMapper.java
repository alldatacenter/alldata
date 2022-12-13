package cn.datax.service.data.visual.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.data.visual.api.dto.DataSetDto;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.vo.DataSetVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据集信息表 Mapper 实体映射
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Mapper(componentModel = "spring")
public interface DataSetMapper extends EntityMapper<DataSetDto, DataSetEntity, DataSetVo> {

}
