package com.alibaba.tesla.appmanager.domain.res.deployconfig;

import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployConfigGenerateRes {

    /**
     * 生成结果内容
     */
    private DeployAppSchema schema;
}
