/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Map;
import java.util.Objects;

/**
 * JSON model that describes a Single Message Transform (SMT) entry.
 */
public class TransformsInfo {

    private final String className;
    private final Map<String, TransformPropertyDescriptor> properties;

    @JsonbCreator
    public TransformsInfo(@JsonbProperty("transform") String className, @JsonbProperty Map<String, TransformPropertyDescriptor> properties) {
        this.className = className;
        this.properties = properties;
    }

    @JsonbProperty("transform")
    public String getClassName() {
        return this.className;
    }

    @JsonbProperty
    public Map<String, TransformPropertyDescriptor> getProperties() {
        return this.properties;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        else if (o != null && this.getClass() == o.getClass()) {
            TransformsInfo that = (TransformsInfo) o;
            return Objects.equals(this.className, that.className)
                    && Objects.equals(this.properties, that.properties);
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.className, this.properties);
    }

    public String toString() {
        return "TransformsInfo{" + "className='" + this.className + '\'' +
                ", properties='" + this.properties + '\'' +
                '}';
    }
}
