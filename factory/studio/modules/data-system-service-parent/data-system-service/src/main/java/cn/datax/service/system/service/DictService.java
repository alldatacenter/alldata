package cn.datax.service.system.service;

import cn.datax.service.system.api.entity.DictEntity;
import cn.datax.service.system.api.dto.DictDto;
import cn.datax.common.base.BaseService;
import cn.datax.service.system.api.entity.DictItemEntity;

import java.util.List;

/**
 * <p>
 * 字典编码信息表 服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
public interface DictService extends BaseService<DictEntity> {

    DictEntity saveDict(DictDto dict);

    DictEntity updateDict(DictDto dict);

    void deleteDictById(String id);

    void deleteDictBatch(List<String> ids);

    List<DictItemEntity> getDictItems(String code);

    void refreshDict();
}
