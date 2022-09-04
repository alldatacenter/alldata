package com.alibaba.tesla.appmanager.trait.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.TraitDTO;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import org.springframework.stereotype.Component;

/**
 * Trait DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class TraitDtoConvert extends BaseDtoConvert<TraitDTO, TraitDO> {

    public TraitDtoConvert() {
        super(TraitDTO.class, TraitDO.class);
    }
}
