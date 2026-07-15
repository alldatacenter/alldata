package cn.datax.service.system.service;

import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.api.dto.DictItemDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 字典项信息表 服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
public interface DictItemService extends BaseService<DictItemEntity> {

    DictItemEntity saveDictItem(DictItemDto dictItem);

    DictItemEntity updateDictItem(DictItemDto dictItem);

    void deleteDictItemById(String id);

    void deleteDictItemBatch(List<String> ids);
}
