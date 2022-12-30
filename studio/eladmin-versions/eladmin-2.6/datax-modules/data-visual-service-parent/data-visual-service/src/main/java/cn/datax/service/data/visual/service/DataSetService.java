package cn.datax.service.data.visual.service;

import cn.datax.service.data.visual.api.dto.SqlParseDto;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.dto.DataSetDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 数据集信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
public interface DataSetService extends BaseService<DataSetEntity> {

    DataSetEntity saveDataSet(DataSetDto dataSet);

    DataSetEntity updateDataSet(DataSetDto dataSet);

    DataSetEntity getDataSetById(String id);

    void deleteDataSetById(String id);

    void deleteDataSetBatch(List<String> ids);

    List<String> sqlAnalyse(SqlParseDto sqlParseDto);

	DataSetEntity getBySourceId(String sourceId);
}
