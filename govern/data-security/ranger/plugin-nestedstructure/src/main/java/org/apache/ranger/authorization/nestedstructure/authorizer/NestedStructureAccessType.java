/**
* Copyright 2022 Comcast Cable Communications Management, LLC
*
* Licensed under the Apache License, Version 2.0 (the ""License"");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an ""AS IS"" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or   implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* SPDX-License-Identifier: Apache-2.0
*/

package org.apache.ranger.authorization.nestedstructure.authorizer;

import org.apache.commons.lang3.StringUtils;

public enum NestedStructureAccessType {
    READ("read"),
    WRITE("write");

    private final String value;

    public static NestedStructureAccessType getAccessType(String name) {
        for (NestedStructureAccessType accessType : NestedStructureAccessType.values()) {
            if (StringUtils.equalsIgnoreCase(accessType.value, name) ||
                StringUtils.equalsIgnoreCase(accessType.name(), name)) {
                return accessType;
            }
        }

        return null;
    }

    NestedStructureAccessType(String value) {
        this.value = value;
    }

    public String getValue() { return value; }
}
