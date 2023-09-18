package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ServiceRoleRunner implements Serializable {
    private String timeout;

    private String program;

    private List<String> args;
}
