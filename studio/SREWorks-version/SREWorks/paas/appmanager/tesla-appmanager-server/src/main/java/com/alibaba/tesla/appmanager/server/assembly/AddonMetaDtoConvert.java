package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.fastjson.JSON;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.AddonMetaDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Addon Meta DTO Converter
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class AddonMetaDtoConvert extends BaseDtoConvert<AddonMetaDTO, AddonMetaDO> {

    public AddonMetaDtoConvert() {
        super(AddonMetaDTO.class, AddonMetaDO.class);
    }

    @Override
    public AddonMetaDTO to(AddonMetaDO addonMetaDO) {
        if (addonMetaDO == null) {
            return null;
        }
        AddonMetaDTO result = new AddonMetaDTO();
        ClassUtil.copy(addonMetaDO, result);
        if (StringUtils.isNotEmpty(addonMetaDO.getAddonConfigSchema())) {
            result.setComponentsSchema(JSON.parseObject(addonMetaDO.getAddonConfigSchema()));
        } else {
            result.setComponentsSchema(null);
        }
        return result;
    }
}
