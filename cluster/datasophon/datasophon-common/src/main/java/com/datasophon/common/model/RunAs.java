package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RunAs implements Serializable {
    private String user;

    private String group;
}
