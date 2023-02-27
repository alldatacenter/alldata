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

package org.apache.atlas.model.notification;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Represents the version of a notification message.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class MessageVersion implements Comparable<MessageVersion>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Used for message with no version (old format).
     */
    public static final MessageVersion NO_VERSION = new MessageVersion("0");
    public static final MessageVersion VERSION_1  = new MessageVersion("1.0.0");

    public static final MessageVersion CURRENT_VERSION = VERSION_1;

    private String version;


    // ----- Constructors ----------------------------------------------------
    public MessageVersion() {
        this.version = CURRENT_VERSION.version;
    }

    /**
     * Create a message version.
     *
     * @param version  the version string
     */
    public MessageVersion(String version) {
        this.version = version;

        try {
            getVersionParts();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid version string : %s.", version), e);
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    // ----- Comparable ------------------------------------------------------

    @Override
    public int compareTo(MessageVersion that) {
        if (that == null) {
            return 1;
        }

        Integer[] thisParts = getVersionParts();
        Integer[] thatParts = that.getVersionParts();

        int length = Math.max(thisParts.length, thatParts.length);

        for (int i = 0; i < length; i++) {

            int comp = getVersionPart(thisParts, i) - getVersionPart(thatParts, i);

            if (comp != 0) {
                return comp;
            }
        }
        return 0;
    }


    // ----- Object overrides ------------------------------------------------

    @Override
    public boolean equals(Object that) {
        if (this == that){
            return true;
        }

        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        return compareTo((MessageVersion) that) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getVersionParts());
    }


    @Override
    public String toString() {
        return "MessageVersion[version=" + version + "]";
    }

    // ----- helper methods --------------------------------------------------

    /**
     * Get the version parts array by splitting the version string.
     * Strip the trailing zeros (i.e. '1.0.0' equals '1').
     *
     * @return  the version parts array
     */
    public Integer[] getVersionParts() {

        String[] sParts = version.split("\\.");
        ArrayList<Integer> iParts = new ArrayList<>();
        int trailingZeros = 0;

        for (String sPart : sParts) {
            Integer iPart = new Integer(sPart);

            if (iPart == 0) {
                ++trailingZeros;
            } else {
                for (int i = 0; i < trailingZeros; ++i) {
                    iParts.add(0);
                }
                trailingZeros = 0;
                iParts.add(iPart);
            }
        }
        return iParts.toArray(new Integer[iParts.size()]);
    }

    public Integer getVersionPart(Integer[] versionParts, int i) {
        return i < versionParts.length ? versionParts[i] : 0;
    }
}
