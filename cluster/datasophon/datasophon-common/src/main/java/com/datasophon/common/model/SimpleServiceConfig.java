package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SimpleServiceConfig implements Serializable {
    private String name;

    private Object value;

}
