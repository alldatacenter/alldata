package cn.datax.service.data.standard.service;

import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.dto.ContrastDictDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 字典对照信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
public interface ContrastDictService extends BaseService<ContrastDictEntity> {

    ContrastDictEntity saveContrastDict(ContrastDictDto contrastDict);

    ContrastDictEntity updateContrastDict(ContrastDictDto contrastDict);

    ContrastDictEntity getContrastDictById(String id);

    void deleteContrastDictById(String id);

    void deleteContrastDictBatch(List<String> ids);
}
