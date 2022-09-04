package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.UnitDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Unit DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class UnitDtoConvert extends BaseDtoConvert<UnitDTO, UnitDO> {

    public UnitDtoConvert() {
        super(UnitDTO.class, UnitDO.class);
    }
}
