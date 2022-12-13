package cn.datax.service.data.standard.service;

import cn.datax.service.data.standard.api.entity.TypeEntity;
import cn.datax.service.data.standard.api.dto.TypeDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 数据标准类别表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
public interface TypeService extends BaseService<TypeEntity> {

    TypeEntity saveType(TypeDto type);

    TypeEntity updateType(TypeDto type);

    TypeEntity getTypeById(String id);

    void deleteTypeById(String id);

    void deleteTypeBatch(List<String> ids);
}
