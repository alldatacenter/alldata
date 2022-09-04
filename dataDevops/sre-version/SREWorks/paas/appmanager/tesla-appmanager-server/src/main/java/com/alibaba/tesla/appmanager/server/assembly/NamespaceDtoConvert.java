package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.NamespaceDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class NamespaceDtoConvert extends BaseDtoConvert<NamespaceDTO, NamespaceDO> {

    public NamespaceDtoConvert() {
        super(NamespaceDTO.class, NamespaceDO.class);
    }
}
