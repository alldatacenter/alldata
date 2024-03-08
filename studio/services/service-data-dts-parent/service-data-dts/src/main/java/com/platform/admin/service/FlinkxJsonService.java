package com.platform.admin.service;

import com.platform.admin.dto.FlinkXJsonBuildDto;


public interface FlinkxJsonService {

    /**
     * build flinkx json
     *
     * @param dto
     * @return
     */
    String buildJobJson(FlinkXJsonBuildDto dto);
}
