package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppAddonDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * App Addon DTO Converter
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class AppAddonDtoConvert extends BaseDtoConvert<AppAddonDTO, AppAddonDO> {

    public AppAddonDtoConvert() {
        super(AppAddonDTO.class, AppAddonDO.class);
    }

    @Override
    public AppAddonDTO to(AppAddonDO appAddonDO) {
        if (appAddonDO == null) {
            return null;
        }
        AppAddonDTO result = new AppAddonDTO();
        ClassUtil.copy(appAddonDO, result);
        if (StringUtils.isNotEmpty(appAddonDO.getAddonConfig())) {
            result.setSpec(JSON.parseObject(appAddonDO.getAddonConfig()));
        } else {
            result.setSpec(new JSONObject());
        }
        return result;
    }

    @Override
    public AppAddonDO from(AppAddonDTO appAddonDTO) {
        if (appAddonDTO == null) {
            return null;
        }
        AppAddonDO result = new AppAddonDO();
        ClassUtil.copy(appAddonDTO, result);
        if (Objects.isNull(appAddonDTO.getSpec())) {
            result.setAddonConfig("{}");
        } else {
            result.setAddonConfig(appAddonDTO.getSpec().toJSONString());
        }
        return result;
    }
}
