package com.datasophon.common.model;

import lombok.Data;

import java.util.List;

@Data
public class ConfigWriter {
    private List<Generators> generators;
}
