package com.alibaba.tesla.gateway.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SwitchViewMapping implements Serializable {
    private static final long serialVersionUID = 3878550945793929121L;

    private String fromEmpId;

    private String fromLoginName;

    private String fromBucId;

    private String toEmpId;

    private String toLoginName;

    private String toBucId;
}