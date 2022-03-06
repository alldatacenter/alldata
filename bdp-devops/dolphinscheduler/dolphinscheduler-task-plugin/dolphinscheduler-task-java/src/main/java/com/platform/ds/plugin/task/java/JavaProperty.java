package com.platform.ds.plugin.task.java;

import java.util.Objects;

public class JavaProperty {
    /**
     * key
     */
    private String prop;

    /**
     * value
     */
    private String value;

    public JavaProperty() {
    }

    public JavaProperty(String prop, String value) {
        this.prop = prop;
        this.value = value;
    }

    /**
     * getter method
     *
     * @return the prop
     * @see JavaProperty#prop
     */
    public String getProp() {
        return prop;
    }

    /**
     * setter method
     *
     * @param prop the prop to set
     * @see JavaProperty#prop
     */
    public void setProp(String prop) {
        this.prop = prop;
    }

    /**
     * getter method
     *
     * @return the value
     * @see JavaProperty#value
     */
    public String getValue() {
        return value;
    }

    /**
     * setter method
     *
     * @param value the value to set
     * @see JavaProperty#value
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JavaProperty property = (JavaProperty) o;
        return Objects.equals(prop, property.prop)
                && Objects.equals(value, property.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, value);
    }

    @Override
    public String toString() {
        return "JavaProperty{"
                + "prop='" + prop + '\''
                + ", value='" + value + '\''
                + '}';
    }

}
