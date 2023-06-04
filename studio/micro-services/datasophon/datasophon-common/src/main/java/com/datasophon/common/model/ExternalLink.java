package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExternalLink implements Serializable {
    private String name;

    private String label;

    private String url;
}
