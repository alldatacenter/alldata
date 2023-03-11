package com.platform.admin.service;

import com.platform.admin.dto.FlinkXJsonBuildDto;

/**
 * com.platform json构建服务层接口
 *
 * @author AllDataDC
 * @version 1.0
 * @since 2023/01/1
 */
public interface FlinkxJsonService {

    /**
     * build flinkx json
     *
     * @param dto
     * @return
     */
    String buildJobJson(FlinkXJsonBuildDto dto);
}
