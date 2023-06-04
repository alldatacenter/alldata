package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ServiceConfig  implements Serializable {
    private String name;

    private Object value;

    private String label;

    private String description;

    private boolean required;

    private String type;

    private boolean configurableInWizard;

    private Object defaultValue;

    private Integer minValue;

    private Integer maxValue;

    private String unit;

    private boolean hidden;

    private List<String> selectValue;

    private String configType;

    private boolean configWithKerberos;

    private boolean configWithRack;

    private boolean configWithHA;

    private String separator;

}
