package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.ClusterDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cluster DTO Converter
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class ClusterDtoConvert extends BaseDtoConvert<ClusterDTO, ClusterDO> {

    public ClusterDtoConvert() {
        super(ClusterDTO.class, ClusterDO.class);
    }
}
