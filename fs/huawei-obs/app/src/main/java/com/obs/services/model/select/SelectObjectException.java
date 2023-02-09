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

import com.obs.services.exception.ObsException;

/**
 * Select Object Content exception
 */
public class SelectObjectException extends ObsException {
    /**
     * Constructor
     * 
     * @param code
     *      Error code
     * 
     * @param message
     *      Error message
     */
    public SelectObjectException(String code, String message) {
        super(message);
        setErrorCode(code);
        setErrorMessage(message);
        setResponseStatus("error");
    }

    /**
     * Returns the error code
     * 
     * @returns Error code
     */
    public String getErrorCode() {
        return super.getErrorCode();
    }

    /**
     * Returns the error message
     * 
     * @returns Error message
     */
    public String getErrorMessage() {
        return super.getErrorMessage();
    }

    /**
     * Formats the error to string
     * 
     * @return String with the error code and message
     */
    @Override
    public String toString() {
        return "ErrorCode: " + getErrorCode() + ", ErrorMessage: " + getErrorMessage();
    }
}
