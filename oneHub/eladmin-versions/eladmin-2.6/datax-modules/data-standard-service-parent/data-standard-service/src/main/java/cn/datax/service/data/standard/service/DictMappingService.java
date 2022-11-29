package cn.datax.service.data.standard.service;

import cn.datax.service.data.standard.api.dto.ManualMappingDto;

import java.util.Map;

public interface DictMappingService {

    Map<String, Object> getDictMapping(String id);

    void dictAutoMapping(String id);

    void dictManualMapping(ManualMappingDto manualMappingDto);

    void dictCancelMapping(String id);
}
