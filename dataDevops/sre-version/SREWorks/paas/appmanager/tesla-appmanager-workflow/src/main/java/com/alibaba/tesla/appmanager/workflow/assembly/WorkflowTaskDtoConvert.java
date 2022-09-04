package com.alibaba.tesla.appmanager.workflow.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WorkflowTask DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class WorkflowTaskDtoConvert extends BaseDtoConvert<WorkflowTaskDTO, WorkflowTaskDO> {

    public WorkflowTaskDtoConvert() {
        super(WorkflowTaskDTO.class, WorkflowTaskDO.class);
    }
}
