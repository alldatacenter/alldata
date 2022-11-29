package cn.datax.service.file.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.file.api.dto.FileDto;
import cn.datax.service.file.api.entity.FileEntity;
import cn.datax.service.file.api.vo.FileVo;
import org.mapstruct.Mapper;

/**
 * <p>
 * 数据集信息表 Mapper 实体映射
 * </p>
 *
 * @author yuwei
 * @since 2020-03-20
 */
@Mapper(componentModel = "spring")
public interface FileMapper extends EntityMapper<FileDto, FileEntity, FileVo> {

}
