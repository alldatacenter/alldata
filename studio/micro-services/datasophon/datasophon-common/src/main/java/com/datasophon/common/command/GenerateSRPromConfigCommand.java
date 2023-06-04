package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateSRPromConfigCommand implements Serializable {

    private Integer serviceInstanceId;

    private String clusterFrame;

    private Integer clusterId;

    private String filename;

}
