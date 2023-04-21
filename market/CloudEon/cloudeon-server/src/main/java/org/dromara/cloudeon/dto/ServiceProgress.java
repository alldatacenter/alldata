package org.dromara.cloudeon.dto;

import org.dromara.cloudeon.entity.CommandTaskEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class ServiceProgress {
    private String currentState;
    private String serviceInstanceName;
    private List<CommandTaskEntity> taskDetails;
    private Long totalCnt;
    private Long successCnt;
}
