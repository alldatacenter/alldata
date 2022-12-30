package cn.datax.service.data.masterdata.service;

import cn.datax.service.data.masterdata.api.entity.ModelDataEntity;
import cn.datax.service.data.masterdata.api.query.ModelDataQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface ModelDataService {

    IPage<Map<String, Object>> getPageModelDatas(ModelDataQuery modelDataQuery);

    void addModelData(ModelDataEntity modelDataEntity);

    void updateModelData(ModelDataEntity modelDataEntity);

    void delModelData(ModelDataEntity modelDataEntity);

    Map<String, Object> getModelDataById(ModelDataEntity modelDataEntity);
}
