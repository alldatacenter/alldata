package cn.datax.service.codegen.service;

import cn.datax.common.base.BaseService;
import cn.datax.service.codegen.api.dto.GenTableDto;
import cn.datax.service.codegen.api.entity.GenTableEntity;

/**
 * <p>
 * 代码生成信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
public interface GenTableService extends BaseService<GenTableEntity> {

    void saveGenTable(GenTableDto genTable);

    void updateGenTable(GenTableDto genTable);

    GenTableEntity getGenTableById(String id);

    void deleteGenTableById(String id);
}
