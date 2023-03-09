package com.linkedin.feathr.common;
import com.linkedin.feathr.offline.client.TypedRef;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a fully-qualified reference to a feature.
 */
public class FeatureRef implements Serializable {

    /**
     * Constructor
     * @param namespace String representation of namespace of referenced feature
     * @param name Name of referenced feature
     * @param major Major version of referenced feature
     * @param minor Minor version of referenced feature
     */
    public FeatureRef(String namespace, String name, int major, int minor) {

        _namespace = namespace != null ? namespace : null;
        _name = name;
        _major = major;
        _minor = minor;
    }

    /**
     * Constructor to create FeatureRef from a string representation.
     * @param featureRefString A string representation of the feature reference
     */
    public FeatureRef(String featureRefString) {
        _namespace = null;
        _name = featureRefString;
        _major = 0;
        _minor = 0;
    }
    private final String _namespace;
    private final String _name;
    private final int _major;
    private final int _minor;
    private String _str;

    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        if (_str == null) {
            StringBuilder bldr = new StringBuilder();

            if (_namespace != null) {
                bldr.append(_namespace).append(DELIM);
            }

            bldr.append(_name);
            _str = bldr.toString();
        }

        return _str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureRef typedRef = (FeatureRef) o;
        return Objects.equals(_namespace, typedRef._namespace) && Objects.equals(_name, typedRef._name)
                && Objects.equals(_minor, typedRef._minor) && Objects.equals(_major, typedRef._major);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_namespace, _name, _major, _minor);
    }
    public static final String DELIM = "-";
}
