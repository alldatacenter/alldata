package cn.datax.service.data.standard.service;

import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.data.standard.api.dto.DictDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 数据标准字典表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
public interface DictService extends BaseService<DictEntity> {

    DictEntity saveDict(DictDto dict);

    DictEntity updateDict(DictDto dict);

    DictEntity getDictById(String id);

    void deleteDictById(String id);

    void deleteDictBatch(List<String> ids);

    void refreshDict();
}
