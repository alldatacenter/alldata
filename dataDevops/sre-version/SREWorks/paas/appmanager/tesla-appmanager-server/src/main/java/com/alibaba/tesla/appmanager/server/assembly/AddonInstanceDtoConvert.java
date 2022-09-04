package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.AddonInstanceDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class AddonInstanceDtoConvert extends BaseDtoConvert<AddonInstanceDTO, AddonInstanceDO> {

    public AddonInstanceDtoConvert() {
        super(AddonInstanceDTO.class, AddonInstanceDO.class);
    }
}
