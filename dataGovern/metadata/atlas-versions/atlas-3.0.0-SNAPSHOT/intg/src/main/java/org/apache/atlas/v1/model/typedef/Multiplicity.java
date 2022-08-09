/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.v1.model.typedef;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(using = Multiplicity.MultiplicitySerializer.class, include=JsonSerialize.Inclusion.NON_NULL)
@JsonDeserialize(using = Multiplicity.MultiplicityDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Multiplicity implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final Multiplicity OPTIONAL   = new Multiplicity(0, 1, false);
    public static final Multiplicity REQUIRED   = new Multiplicity(1, 1, false);
    public static final Multiplicity COLLECTION = new Multiplicity(1, Integer.MAX_VALUE, false);
    public static final Multiplicity SET        = new Multiplicity(1, Integer.MAX_VALUE, true);

    private int     lower;
    private int     upper;
    private boolean isUnique;

    public Multiplicity() {
        this(Multiplicity.REQUIRED);
    }

    public Multiplicity(Multiplicity copyFrom) {
        this(copyFrom.lower, copyFrom.upper, copyFrom.isUnique);
    }

    public Multiplicity(int lower, int upper, boolean isUnique) {
        this.lower    = lower;
        this.upper    = upper;
        this.isUnique = isUnique;
    }

    public int getLower() {
        return lower;
    }

    public void setLower(int lower) {
        this.lower = lower;
    }

    public int getUpper() {
        return upper;
    }

    public void setUpper(int upper) {
        this.upper = upper;
    }

    public boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(boolean isUnique) {
        this.isUnique = isUnique;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Multiplicity that = (Multiplicity) o;

        return lower == that.lower &&
               upper == that.upper &&
               isUnique == that.isUnique;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper, isUnique);
    }


    static class MultiplicitySerializer extends JsonSerializer<Multiplicity> {
        @Override
        public void serialize(Multiplicity value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value != null) {
                final String serializedValue;

                if (value.getLower() < 1) {
                    serializedValue = "optional";
                } else if (value.equals(Multiplicity.COLLECTION)) {
                    serializedValue = "collection";
                } else if (value.equals(Multiplicity.SET)) {
                    serializedValue = "set";
                } else if (value.equals(Multiplicity.REQUIRED)) {
                    serializedValue = "required";
                } else { // default value
                    serializedValue = "required";
                }

                jgen.writeString(serializedValue);
            }
        }
    }

    static class MultiplicityDeserializer extends JsonDeserializer<Multiplicity> {
        @Override
        public Multiplicity deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            Multiplicity ret = null;

            String value = parser.readValueAs(String.class);

            if (value != null) {
                if (value.equals("required")) {
                    ret = new Multiplicity(Multiplicity.REQUIRED);
                } else if (value.equals("optional")) {
                    ret = new Multiplicity(Multiplicity.OPTIONAL);
                } else if (value.equals("collection")) {
                    ret = new Multiplicity(Multiplicity.COLLECTION);
                } else if (value.equals("set")) {
                    ret = new Multiplicity(Multiplicity.SET);
                }
            }

            if (ret == null) {
                ret = new Multiplicity();
            }

            return ret;
        }
    }
}
