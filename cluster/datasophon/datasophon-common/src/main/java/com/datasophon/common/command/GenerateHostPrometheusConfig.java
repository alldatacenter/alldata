package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateHostPrometheusConfig  implements Serializable {

    private Integer clusterId;
}
