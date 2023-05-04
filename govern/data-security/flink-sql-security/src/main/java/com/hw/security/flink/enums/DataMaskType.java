package com.hw.security.flink.enums;

import lombok.Data;

import java.util.Map;

/**
 * @description: DataMaskType
 * @author: HamaWhite
 */
@Data
public class DataMaskType {

    private Long itemId;

    private String name;

    private String label;

    private String description;

    private String transformer;

    private Map<String, String> dataMaskOptions;
}
