package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.RtAppInstanceHistoryDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 实时应用实例历史 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class RtAppInstanceHistoryDtoConvert extends BaseDtoConvert<RtAppInstanceHistoryDTO, RtAppInstanceHistoryDO> {

    public RtAppInstanceHistoryDtoConvert() {
        super(RtAppInstanceHistoryDTO.class, RtAppInstanceHistoryDO.class);
    }
}
