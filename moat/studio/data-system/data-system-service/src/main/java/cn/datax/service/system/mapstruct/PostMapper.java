package cn.datax.service.system.mapstruct;

import cn.datax.common.mapstruct.EntityMapper;
import cn.datax.service.system.api.dto.PostDto;
import cn.datax.service.system.api.entity.PostEntity;
import cn.datax.service.system.api.vo.PostVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper extends EntityMapper<PostDto, PostEntity, PostVo> {
}
