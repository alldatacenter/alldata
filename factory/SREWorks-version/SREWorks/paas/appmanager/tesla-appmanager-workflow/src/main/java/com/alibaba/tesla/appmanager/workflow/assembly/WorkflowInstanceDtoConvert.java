package com.alibaba.tesla.appmanager.workflow.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowInstanceDTO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WorkflowInstance DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class WorkflowInstanceDtoConvert extends BaseDtoConvert<WorkflowInstanceDTO, WorkflowInstanceDO> {

    public WorkflowInstanceDtoConvert() {
        super(WorkflowInstanceDTO.class, WorkflowInstanceDO.class);
    }
}
