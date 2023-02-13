package com.alibaba.tesla.appmanager.deployconfig.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDO;
import com.alibaba.tesla.appmanager.domain.dto.DeployConfigHistoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deploy Config History DTO Converter
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class DeployConfigHistoryDtoConvert extends BaseDtoConvert<DeployConfigHistoryDTO, DeployConfigHistoryDO> {

    public DeployConfigHistoryDtoConvert() {
        super(DeployConfigHistoryDTO.class, DeployConfigHistoryDO.class);
    }
}
