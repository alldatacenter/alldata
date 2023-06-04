package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class HdfsEcCommand implements Serializable {

    private Integer serviceInstanceId;
}

