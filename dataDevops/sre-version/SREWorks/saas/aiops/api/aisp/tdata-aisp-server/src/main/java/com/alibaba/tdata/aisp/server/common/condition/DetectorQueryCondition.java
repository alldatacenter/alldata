package com.alibaba.tdata.aisp.server.common.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: DetectorQueryCondition
 * @Author: dyj
 * @DATE: 2021-11-23
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectorQueryCondition {
    private String detectorCode;
}
