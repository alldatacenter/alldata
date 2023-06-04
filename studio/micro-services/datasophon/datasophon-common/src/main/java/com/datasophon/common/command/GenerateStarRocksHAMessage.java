package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateStarRocksHAMessage implements Serializable {
    private Integer serviceInstanceId;

    private Integer clusterId;
}
