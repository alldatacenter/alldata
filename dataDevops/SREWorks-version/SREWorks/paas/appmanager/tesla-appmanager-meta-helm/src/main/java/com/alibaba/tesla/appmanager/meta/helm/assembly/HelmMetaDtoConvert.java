package com.alibaba.tesla.appmanager.meta.helm.assembly;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.PackageTypeEnum;
import com.alibaba.tesla.appmanager.domain.dto.HelmMetaDTO;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HELM组件 DTO<==>DO 转换器
 */
@Component
public class HelmMetaDtoConvert extends BaseDtoConvert<HelmMetaDTO, HelmMetaDO>{
    public HelmMetaDtoConvert() {
        super(HelmMetaDTO.class, HelmMetaDO.class);
    }

    @Override
    public HelmMetaDTO to(HelmMetaDO source) {
        HelmMetaDTO helmMetaDTO = super.to(source);
        helmMetaDTO.setComponentType(ComponentTypeEnum.valueOf(source.getComponentType()));
        helmMetaDTO.setPackageType(PackageTypeEnum.valueOf(source.getPackageType()));
        helmMetaDTO.setHelmExt(JSONObject.parseObject(source.getHelmExt()));
        return helmMetaDTO;
    }

    @Override
    public List<HelmMetaDTO> to(List<HelmMetaDO> sourceList) {
        if (sourceList == null || sourceList.size() == 0) {
            return new ArrayList<>();
        }
        List<HelmMetaDTO> helmMetaDTOList = new ArrayList<>();
        for (HelmMetaDO p : sourceList) {
            helmMetaDTOList.add(this.to(p));
        }
        return helmMetaDTOList;
    }

    @Override
    public HelmMetaDO from(HelmMetaDTO source) {
        HelmMetaDO helmMetaDO = super.from(source);
        helmMetaDO.setComponentType(source.getComponentType().name());
        helmMetaDO.setPackageType(source.getPackageType().name());
        helmMetaDO.setHelmExt(source.getHelmExt().toJSONString());
        return helmMetaDO;
    }

    @Override
    public List<HelmMetaDO> from(List<HelmMetaDTO> sourceList) {
        if (sourceList == null || sourceList.size() == 0) {
            return Collections.emptyList();
        }
        List<HelmMetaDO> helmMetaDOList = new ArrayList<>();
        for (HelmMetaDTO d : sourceList) {
            helmMetaDOList.add(this.from(d));
        }
        return helmMetaDOList;
    }
}
