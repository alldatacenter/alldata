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

package com.obs.services.model.fs;

import com.obs.services.model.ObjectMetadata;

/**
 * File or folder properties
 *
 */
public class ObsFSAttribute extends ObjectMetadata {
    private int mode = -1;

    /**
     * Query the file or folder type.
     * 
     * @return File or folder type
     */
    public int getMode() {
        return mode;
    }

    /**
     * Set the file or folder type.
     * 
     * @param mode
     *            File or folder type
     */
    public void setMode(int mode) {
        this.mode = mode;
    }
}
