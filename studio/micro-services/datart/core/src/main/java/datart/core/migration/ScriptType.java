/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.core.migration;

import org.apache.commons.lang3.StringUtils;

public enum ScriptType {

    UPGRADE("V"),

    ROLLBACK("R");

    private final String prefix;

    ScriptType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static ScriptType of(String version) {
        if (StringUtils.isBlank(version)) {
            return null;
        }
        if (version.startsWith(UPGRADE.getPrefix())) {
            return UPGRADE;
        } else if (version.startsWith(ROLLBACK.getPrefix())) {
            return ROLLBACK;
        } else {
            return null;
        }
    }
}
