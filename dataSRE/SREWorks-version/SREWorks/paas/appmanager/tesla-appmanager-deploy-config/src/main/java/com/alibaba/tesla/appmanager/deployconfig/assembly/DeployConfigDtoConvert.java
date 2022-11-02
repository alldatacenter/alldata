package com.alibaba.tesla.appmanager.deployconfig.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.domain.dto.DeployConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deploy Config DTO Converter
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class DeployConfigDtoConvert extends BaseDtoConvert<DeployConfigDTO, DeployConfigDO> {

    public DeployConfigDtoConvert() {
        super(DeployConfigDTO.class, DeployConfigDO.class);
    }
}
