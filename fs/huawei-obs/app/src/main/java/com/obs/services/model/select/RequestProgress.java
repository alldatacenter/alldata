/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

import com.obs.services.internal.xml.OBSXMLBuilder;

/**
 * Specifies if periodic progress information has to be enabled
 */
public class RequestProgress {
    private Boolean enabled;

    /**
     * Enables or disables the periodic progress information
     * 
     * @param enabled
     *      Enable or disable flag
     * 
     * @return Self
     */
    public RequestProgress withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder requestBuilder = xmlBuilder.elem("RequestProgress");
        if (enabled != null) {
            requestBuilder.elem("Enabled").text(enabled.toString());
        }
    }
}
