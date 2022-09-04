package com.alibaba.tesla.appmanager.server.dag.helper;

import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentTrait {

    /**
     * component
     */
    private DeployAppSchema.SpecComponent component;

    /**
     * trait
     */
    private DeployAppSchema.SpecComponentTrait trait;
}