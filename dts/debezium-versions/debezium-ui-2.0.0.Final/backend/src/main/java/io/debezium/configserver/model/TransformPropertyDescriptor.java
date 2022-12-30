/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import javax.json.bind.annotation.JsonbProperty;
import java.util.List;

/**
 * JSON model that describes a property of a Single Message Transform (SMT).
 */
public class TransformPropertyDescriptor {

    @JsonbProperty
    public String title;

    @JsonbProperty("x-name")
    public String name;

    @JsonbProperty
    public String description;

    @JsonbProperty
    public String type;

    @JsonbProperty("enum")
    public List<String> allowedValues;

    @JsonbProperty
    public String format;

    @JsonbProperty
    public String defaultValue;

    public TransformPropertyDescriptor() { }

    public String toString() {
        return "TransformPropertyDescriptor{" + "title='" + this.title + '\'' +
                ", x-name='" + this.name + '\'' +
                ", description='" + this.description + '\'' +
                ", type='" + this.type + '\'' +
                ", enum='" + this.allowedValues + '\'' +
                ", format='" + this.format + '\'' +
                ", defaultValue='" + this.defaultValue + '\'' +
                '}';
    }
}
